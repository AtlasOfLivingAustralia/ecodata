package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.*
import au.org.ala.ws.tokens.TokenService
import grails.converters.JSON
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
/**
 * Supports the implementation of the paratoo "org" interface
 */
@Slf4j
class ParatooService {

    static final String PARATOO_PROTOCOL_PATH = '/protocols'
    static final String PARATOO_PROTOCOL_FORM_TYPE = 'EMSA'
    static final String PARATOO_PROTOCOLS_KEY = 'paratoo.protocols'
    static final String PARATOO_PROTOCOL_DATA_MAPPING_KEY = 'paratoo.surveyData.mapping'
    static final String PROGRAM_CONFIG_PARATOO_ITEM = 'supportsParatoo'
    static final String PARATOO_APP_NAME = "Monitor"
    static final String MONITOR_AUTH_HEADER = "Authorization"
    static final List DEFAULT_MODULES =
            ['Plot Selection and Layout', 'Plot Description']

    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService
    ProjectService projectService
    SiteService siteService
    PermissionService permissionService
    TokenService tokenService

    /**
     * The rules we use to find projects eligible for use by paratoo are:
     * 1. The project is under a program that has flagged eligibility for paratoo
     * 2. The project is currently active (status = active)
     * 3. The project has protocols selected.  (The current way this is implemented is via a mapping
     * to project services.  A hypothetical future implementation for BioCollect could include finding
     * a ProjectActivity with a compatible activity type)
     *
     * @param userId The user of interest
     * @param includeProtocols
     * @return
     */
    List<ParatooProject> userProjects(String userId) {

        List<ParatooProject> projects = findUserProjects(userId)

        projects.each { ParatooProject project ->
            project.protocols = findProjectProtocols(project)
        }

        projects.findAll{it.protocols}
    }

    private static List findProjectProtocols(ParatooProject project) {
        log.debug "Finding protocols for ${project.id} ${project.name}"
        List<ActivityForm> protocols = []

        List monitoringProtocolCategories = project.getMonitoringProtocolCategories()
        if (monitoringProtocolCategories) {
            List categoriesWithDefaults = monitoringProtocolCategories + DEFAULT_MODULES
            protocols += findProtocolsByCategories(categoriesWithDefaults.unique())
        }
        protocols
    }

    private static List findProtocolsByCategories(List categories) {
        List<ActivityForm> forms = ActivityForm.findAllByCategoryInListAndExternalAndStatusNotEqual(categories, true, Status.DELETED)
        forms
    }

    private List<ParatooProject> findUserProjects(String userId) {
        List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, Project.class.name, Status.DELETED)


        // If the permission has been set as a favourite then delegate to the Hub permission.
        Map projectAccessLevels = [:]
        permissions?.each { UserPermission permission ->
            String projectId = permission.entityId
            // Don't override an existing permission with a starred permission
            if (permission.accessLevel == AccessLevel.starred) {
                if (!projectAccessLevels[projectId]) {
                    permission = permissionService.findParentPermission(permission)
                    projectAccessLevels[projectId] = permission?.accessLevel
                }
            }
            else {
                // Update the map of projectId to accessLevel
                projectAccessLevels[projectId] = permission.accessLevel
            }
        }

        List projects = Project.findAllByProjectIdInListAndStatus(new ArrayList(projectAccessLevels.keySet()), Status.ACTIVE)

        // Filter projects that aren't in a program configured to support paratoo or don't have permission
        projects = projects.findAll { Project project ->
            if (!project.programId) {
                return false
            }

            Program program = Program.findByProgramId(project.programId)
            Map config = program.getInheritedConfig()
            // The Monitor/Paratoo app is "write only" (i.e. there is no view mode for the data), so we don't support
            // the read only role
            config?.get(PROGRAM_CONFIG_PARATOO_ITEM) && projectAccessLevels[project.projectId] && projectAccessLevels[project.projectId] != AccessLevel.readOnly
        }

        List paratooProjects = projects.collect { Project project ->
            List<Site> sites = siteService.sitesForProject(project.projectId)
            AccessLevel accessLevel = projectAccessLevels[project.projectId]
            mapProject(project, accessLevel, sites)
        }
        paratooProjects

    }

    Map mintCollectionId(String userId, ParatooCollectionId paratooCollectionId) {
        String projectId = paratooCollectionId.surveyId.projectId
        Project project = Project.findByProjectId(projectId)

        Map dataSet = mapParatooCollectionId(paratooCollectionId, project)
        dataSet.progress = Activity.PLANNED
        String dataSetName = buildName(paratooCollectionId.surveyId, project)
        dataSet.name = dataSetName

        if (!project.custom) {
            project.custom = [:]
        }
        if (!project.custom.dataSets) {
            project.custom.dataSets = []
        }
        ParatooMintedIdentifier orgMintedIdentifier = new ParatooMintedIdentifier(
                surveyId: paratooCollectionId.surveyId,
                eventTime: new Date(),
                userId: userId,
                projectId: projectId
        )
        dataSet.orgMintedIdentifier = orgMintedIdentifier.encodeAsMintedCollectionId()
        project.custom.dataSets << dataSet
        Map result = projectService.update([custom:project.custom], projectId, false)

        if (!result.error) {
            result.orgMintedIdentifier = dataSet.orgMintedIdentifier
        }
        result
    }

    private static String buildName(ParatooSurveyId surveyId, Project project) {
        ActivityForm protocolForm = ActivityForm.findByExternalId(surveyId.protocol.id)
        String dataSetName = protocolForm?.name + " - " + surveyId.timeAsDisplayDate() + " (" + project.name + ")"
        dataSetName
    }

    Map submitCollection(ParatooCollection collection, ParatooProject project) {

        Map dataSet = project.project.custom?.dataSets?.find{it.orgMintedIdentifier == collection.orgMintedIdentifier}

        if (!dataSet) {
            throw new RuntimeException("Unable to find data set with orgMintedIdentifier: "+collection.orgMintedIdentifier)
        }
        dataSet.progress = Activity.STARTED

        ParatooSurveyId surveyId = ParatooSurveyId.fromMap(dataSet.surveyId)

        ParatooProtocolConfig config = getProtocolConfig(surveyId.protocol.id)
        Map surveyData = retrieveSurveyData(surveyId, config)
        List surveyObservations = retrieveSurveyObservations(surveyId, config)

        if (surveyData) {
            // If we are unable to create a site, null will be returned - assigning a null siteId is valid.
            dataSet.siteId = createSiteFromSurveyData(surveyData, collection, surveyId, project.project, config)
            List species = createSpeciesFromSurveyData(surveyObservations, collection, config, dataSet )
            dataSet.areSpeciesRecorded = species?.size() > 0
            dataSet.startDate = config.getStartDate(surveyData)
            dataSet.endDate = config.getEndDate(surveyData)
        }
        else {
            log.warn("Unable to retrieve survey data for: "+collection.orgMintedIdentifier)
        }

        Map result = projectService.update([custom:project.project.custom], project.id, false)

        result
    }

    private ParatooProtocolConfig getProtocolConfig(String protocolId) {
        String result = settingService.getSetting(PARATOO_PROTOCOL_DATA_MAPPING_KEY)
        Map protocolDataConfig = JSON.parse(result ?: '{}')
        Map config = protocolDataConfig[protocolId]
        new ParatooProtocolConfig(config ?: [:])
    }

    boolean protocolReadCheck(String userId, String projectId, String protocolId) {
        protocolCheck(userId, projectId, protocolId, true)
    }

    boolean protocolWriteCheck(String userId, String projectId, String protocolId) {
        protocolCheck(userId, projectId, protocolId, false)
    }

    private boolean protocolCheck(String userId, String projectId, String protocolId, boolean read) {
        List projects = userProjects(userId)
        ParatooProject project = projects.find{it.id == projectId}
        boolean protocol = project?.protocols?.find{it.externalIds.find{it.externalId == protocolId}}
        int minimumAccess = read ? AccessLevel.projectParticipant.code : AccessLevel.editor.code
        protocol && project.accessLevel.code >= minimumAccess
    }

    Map findDataSet(String userId, String collectionId) {
        List projects = findUserProjects(userId)

        Map dataSet = null
        ParatooProject project = projects?.find {
            dataSet = it.dataSets?.find { it.orgMintedIdentifier == collectionId }
            dataSet
        }
        [dataSet:dataSet, project:project]
    }

    private List createSpeciesFromSurveyData(List surveyObservations, ParatooCollection collection, ParatooProtocolConfig config, Map dataSet) {
        // delete records
        Record.where {
            dataSetId == dataSet.dataSetId
        }.deleteAll()

        createRecords(surveyObservations, config, collection, dataSet)
    }

    private static List createRecords (List surveyObservations, ParatooProtocolConfig config, ParatooCollection collection, Map dataSet) {
        List result = []
        surveyObservations?.each { observation ->
            def obs = transformSpeciesObservation(observation, config, collection, dataSet)
            def record = new Record(obs)
            try {
                record.save(flush: true, failOnError: true)
                result.add(record)
            } catch (Exception e) {
                log.error("Error saving record: ${record.name} ${record.projectId}", e)
            }
        }

        result
    }

    private static Map transformSpeciesObservation (Map observation, ParatooProtocolConfig config, ParatooSurveyId surveyId, ParatooCollection collection, Map dataSet) {
        def lat = config.getDecimalLatitude(observation), lng = config.getDecimalLongitude(observation)
        Map result = [
                dataSetId: dataSet.dataSetId,
                projectId: surveyId.projectId,
                eventDate: config.getEventDate(observation),
                decimalLatitude: lat,
                decimalLongitude: lng,
                individualCount: config.getIndividualCount(observation),
//                numberOfOrganisms: config.getNumberOfOrganisms(observation),
                recordedBy: config.getRecordedBy(observation)
        ]

        result << config.parseSpecies(config.getSpecies(observation))
        result
    }

    private String createSiteFromSurveyData(Map surveyData, ParatooCollection collection, ParatooSurveyId surveyId, Project project, ParatooProtocolConfig config) {
        String siteId = null
        // Create a site representing the location of the collection
        Map geoJson = config.getGeoJson(surveyData)
        if (geoJson) {
            Map siteProps = siteService.propertiesFromGeoJson(geoJson, 'upload')
            siteProps.type = Site.TYPE_SURVEY_AREA
            siteProps.publicationStatus = PublicationStatus.PUBLISHED
            siteProps.projects = [project.projectId]
            Site site = Site.findByTypeAndExternalId(Site.TYPE_SURVEY_AREA, siteProps.externalId)
            Map result
            if (!site) {
                result = siteService.create(siteProps)
            }
            else {
                result = [siteId:site.siteId]
            }
            if (result.error) {  // Don't treat this as a fatal error for the purposes of responding to the paratoo request
                log.error("Error creating a site for survey "+collection.orgMintedIdentifier+", project "+project.projectId+": "+result.error)
            }
            siteId = result.siteId
        }
        siteId
    }

    private Map syncParatooProtocols(List<Map> protocols) {

        Map result = [errors:[], messages:[]]
        protocols.each { Map protocol ->
            String id = protocol.id
            String guid = protocol.attributes.identifier
            String name = protocol.attributes.name
            ActivityForm form = ActivityForm.findByExternalId(guid)
            if (!form) {
                form = new ActivityForm()
                form.externalIds = []
                form.externalIds << new ExternalId(idType: ExternalId.IdType.MONITOR_PROTOCOL_INTERNAL_ID, externalId: id)
                form.externalIds << new ExternalId(idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID, externalId: guid)

                String message = "Creating form with id: "+id+", name: "+name
                result.messages << message
                log.info message
            }
            else {
                String message = "Updating form with id: "+id+", name: "+name
                result.messages << message
                log.info message
            }

            mapProtocolToActivityForm(protocol, form)
            form.save()

            if (form.hasErrors()) {
                result.errors << form.errors
                log.warn "Error saving form with id: "+id+", name: "+name
            }
        }
        result

    }

    /** This is a backup method in case the protocols aren't available online */
    Map syncProtocolsFromSettings() {
        List protocols = JSON.parse(settingService.getSetting(PARATOO_PROTOCOLS_KEY))
        syncParatooProtocols(protocols)
    }

    private String getParatooBaseUrl() {
        grailsApplication.config.getProperty('paratoo.core.baseUrl')
    }


    Map syncProtocolsFromParatoo() {
        String url = paratooBaseUrl+PARATOO_PROTOCOL_PATH
        Map response = webService.getJson(url, null,  null, false)
        syncParatooProtocols(response?.data)
    }

    private static void mapProtocolToActivityForm(Map protocol, ActivityForm form) {
        form.name = protocol.attributes.name
        form.formVersion = protocol.attributes.version
        form.type = PARATOO_PROTOCOL_FORM_TYPE
        form.category = protocol.attributes.module
        form.external = true
        form.publicationStatus = PublicationStatus.PUBLISHED
        form.description = protocol.attributes.description
    }

    private ParatooProject mapProject(Project project, AccessLevel accessLevel, List<Site> sites) {
        Site projectArea = sites.find{it.type == Site.TYPE_PROJECT_AREA}
        Map projectAreaGeoJson = null
        if (projectArea) {
            projectAreaGeoJson = siteService.geometryAsGeoJson(projectArea)
        }

        List<Site> plotSelections = sites.findAll{it.type == Site.TYPE_SURVEY_AREA}

        Map attributes = [
                id:project.projectId,
                name:project.name,
                accessLevel: accessLevel,
                project:project,
                projectArea: projectAreaGeoJson,
                projectAreaSite: projectArea,
                plots: plotSelections]
        new ParatooProject(attributes)

    }

    private static Map mapParatooCollectionId(ParatooCollectionId collectionId, Project project) {
        Map dataSet = [:]
        dataSet.dataSetId = Identifiers.getNew(true, '')
        dataSet.surveyId = collectionId.surveyId.toMap() // No codec to save this to mongo
        dataSet.protocol = collectionId.surveyId.protocol.id
        dataSet.grantId = project.grantId
        dataSet.collectionApp = PARATOO_APP_NAME
        dataSet
    }

    private static String buildSurveyQueryString(int start, int limit) {
        "?populate=deep&sort=updatedAt&pagination[start]=$start&pagination[limit]=$limit"
    }

    Map retrieveSurveyData(ParatooSurveyId surveyId, ParatooProtocolConfig config) {

        String apiEndpoint = config.getApiEndpoint(surveyId)

        String accessToken = tokenService.getAuthToken(true)
        if (!accessToken?.startsWith('Bearer')) {
            accessToken = 'Bearer '+accessToken
        }
        Map authHeader = [(MONITOR_AUTH_HEADER):accessToken]

        if (!accessToken) {
            throw new RuntimeException("Unable to get access token")
        }
        int start = 0
        int limit = 10


        String url = paratooBaseUrl+'/'+apiEndpoint
        String query = buildSurveyQueryString(start, limit)
        Map response = webService.getJson(url+query, null,  authHeader, false)
        Map survey = findMatchingSurvey(surveyId, response.data, config)
        int total = response.meta?.pagination?.total ?: 0
        while (!survey && start+limit < total) {
            start += limit

            query = buildSurveyQueryString(start, limit)
            response = webService.getJson(url+query, null,  authHeader, false)
            survey = findMatchingSurvey(surveyId, response.data, config)
        }

        survey
    }

    List retrieveSurveyObservations(ParatooSurveyId surveyId, ParatooProtocolConfig config) {

        String apiEndpoint = config.observationEndpoint
        String accessToken = tokenService.getAuthToken(true)
        if (!accessToken?.startsWith('Bearer')) {
            accessToken = 'Bearer '+accessToken
        }
        Map authHeader = [(MONITOR_AUTH_HEADER):accessToken]

        if (!accessToken) {
            throw new RuntimeException("Unable to get access token")
        }
        int start = 0
        int limit = 10


        String url = paratooBaseUrl+'/'+apiEndpoint
        String query = buildSurveyQueryString(start, limit)
        Map response = webService.getJson(url+query, null,  authHeader, false)
        List data = config.findObservationsBelongingToSurvey(response.data, surveyId) ?: []
        int total = response.meta?.pagination?.total ?: 0
        while (!data && start+limit < total) {
            start += limit

            query = buildSurveyQueryString(start, limit)
            response = webService.getJson(url+query, null,  authHeader, false)
            data.addAll(config.findObservationsBelongingToSurvey(response.data, surveyId))
        }

        data
    }


    private static Map findMatchingSurvey(ParatooSurveyId surveyId, List data, ParatooProtocolConfig config) {
        data?.find { config.matches(it, surveyId) }
    }

    private static List findSurveyData(ParatooSurveyId surveyId, Map surveyData, ParatooProtocolConfig config) {
        surveyData?.data?.findAll { config.matches(it) }
    }

    Map plotSelections(String userId, Map plotSelectionData) {

        List projects = userProjects(userId)
        if (!projects) {
            return [error:'User has no projects eligible for Monitor site data']
        }

        Map siteData = mapPlotSelection(plotSelectionData)
        // The projects should be specified in the data but they aren't in the swagger so for now we'll
        // assign the site to multiple projects.
        siteData.projects = projects.collect{it.project.projectId}

        Site site = Site.findByExternalId(siteData.externalId)
        Map result
        if (site) {
            result = siteService.update(siteData, site.siteId)
        }
        else {
            result = siteService.create(siteData)
        }

        result
    }

    private static Map mapPlotSelection(Map plotSelectionData) {
        Map geoJson = ParatooProtocolConfig.plotSelectionToGeoJson(plotSelectionData)
        Map site = SiteService.propertiesFromGeoJson(geoJson, 'point')
        site.projects = [] // get all projects for the user I suppose - not sure why this isn't in the payload as it's in the UI...
        site.type = Site.TYPE_SURVEY_AREA

        site
    }

    Map updateProjectSites(ParatooProject project, Map siteData) {
        if (siteData.plot_selections) {
            linkProjectToSites(project, siteData.plot_selections)
        }
        if (siteData.project_area_type && siteData.project_area_coordinates) {
            updateProjectArea(project, siteData.project_area_type, siteData.project_area_coordinates)
        }
    }


    private Map linkProjectToSites(ParatooProject project, List siteExternalIds) {
        List errors = []
        List<Site> sites = Site.findAllByExternalIdInList(siteExternalIds)
        sites.each { Site site ->
            site.projects = site.projects ?: []
            site.projects << project.id
            site.save()
            if (site.hasErrors()) {
                errors << site.errors
            }
        }
        [success:!errors, error:errors]
    }

    private Map updateProjectArea(ParatooProject project, String type, List coordinates) {
        Map geometry = [
                type:type,
                coordinates: coordinates.collect{[it.lng, it.lat]}
        ]
        Site projectArea = project.projectAreaSite
        if (projectArea) {
            projectArea.extent.geometry.type = geometry.type
            projectArea.extent.geometry.coordinates = geometry.coordinates
            siteService.update(projectArea.extent, projectArea.siteId)
        }
        else {

            Map site = [
                    name:'Monitor project area',
                    type:Site.TYPE_PROJECT_AREA,
                    extent: [
                            source:'drawn',
                            geometry:geometry
                    ],
                    projects: [project.id]
            ]
            siteService.create(site)
        }
    }

    // Protocol = 2 (vegetation mapping survey).
        // endpoint /api/vegetation-mapping-surveys is useless
        // (possibly because the protocol is called vegetation-mapping-surveys and there is a module/component inside
        // called vegetation-mapping-survey and the strapi pluralisation is causing issues?)
        // Instead if you query: https://dev.core-api.paratoo.tern.org.au/api/vegetation-mapping-observations?populate=deep
        // You can get multiple observations with different points linked to the same surveyId.

        // Protocol = 7 (drone survey) - No useful spatial data

        // Protocol = 10 & 11 (Photopoints - Compact Panorama & Photopoints - Device Panorama) - same endpoint.
        // Has plot-layout / plot-visit

        // Protocol = 12 (Floristics - enhanced)
        // Has plot-layout / plot-visit

}
