package au.org.ala.ecodata


import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooPlotSelectionData
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooProtocolConfig
import au.org.ala.ecodata.paratoo.ParatooSurveyId
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
            ['Plot Selection and Layout', 'Plot Description', 'Opportune']
    static final List ADMIN_ONLY_PROTOCOLS = ['Plot Selection']
    static final String INTERVENTION_PROTOCOL_TAG = 'intervention'

    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService
    ProjectService projectService
    SiteService siteService
    PermissionService permissionService
    TokenService tokenService
    MetadataService metadataService
    ActivityService activityService

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

    private List findProjectProtocols(ParatooProject project) {
        log.debug "Finding protocols for ${project.id} ${project.name}"
        List<ActivityForm> protocols = []

        List monitoringProtocolCategories = project.getMonitoringProtocolCategories()
        if (monitoringProtocolCategories) {
            List categoriesWithDefaults = monitoringProtocolCategories + DEFAULT_MODULES
            protocols += findProtocolsByCategories(categoriesWithDefaults.unique())
            if (!project.isParaooAdmin()) {
                protocols = protocols.findAll{!(it.name in ADMIN_ONLY_PROTOCOLS)}
            }
            // Temporarily exclude intervention protocols until they are ready
            if (grailsApplication.config.getProperty('paratoo.excludeInterventionProtocols', Boolean.class, true)) {
                protocols = protocols.findAll{!(INTERVENTION_PROTOCOL_TAG in it.tags)}
            }

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
            List<Site> sites = siteService.sitesForProjectWithTypes(project.projectId, [Site.TYPE_PROJECT_AREA, Site.TYPE_SURVEY_AREA])
            AccessLevel accessLevel = projectAccessLevels[project.projectId]
            mapProject(project, accessLevel, sites)
        }
        paratooProjects

    }

    Map mintCollectionId(String userId, ParatooCollectionId paratooCollectionId) {
        String projectId = paratooCollectionId.projectId
        Project project = Project.findByProjectId(projectId)

        // Update the identifier properties as per the org contract and save the data to the data set
        String dataSetId = Identifiers.getNew(true, '')
        paratooCollectionId.eventTime = new Date()
        paratooCollectionId.survey_metadata.orgMintedUUID = dataSetId
        paratooCollectionId.userId = project.organisationId // using the organisation as the owner of the data
        Map dataSet = mapParatooCollectionId(paratooCollectionId, project)

        dataSet.surveyId = paratooCollectionId.toMap() // No codec to save this to mongo

        if (!project.custom) {
            project.custom = [:]
        }
        if (!project.custom.dataSets) {
            project.custom.dataSets = []
        }

        dataSet.orgMintedIdentifier = paratooCollectionId.encodeAsOrgMintedIdentifier()

        log.info "Minting identifier for Monitor collection: ${paratooCollectionId}: ${dataSet.orgMintedIdentifier}"
        project.custom.dataSets << dataSet
        Map result = projectService.update([custom:project.custom], projectId, false)

        if (!result.error) {
            result.orgMintedIdentifier = dataSet.orgMintedIdentifier
        }
        result
    }

    private static String buildName(String protocolId, String displayDate, Project project) {
        ActivityForm protocolForm = ActivityForm.findByExternalId(protocolId)
        String dataSetName = protocolForm?.name + " - " + displayDate + " (" + project.name + ")"
        dataSetName
    }

    Map submitCollection(ParatooCollection collection, ParatooProject project) {

        Map dataSet = project.project.custom?.dataSets?.find{it.dataSetId == collection.orgMintedUUID}

        if (!dataSet) {
            throw new RuntimeException("Unable to find data set with orgMintedUUID: "+collection.orgMintedUUID)
        }
        dataSet.progress = Activity.STARTED
        dataSet.surveyId.coreSubmitTime = new Date()
        dataSet.surveyId.survey_metadata.provenance.putAll(collection.coreProvenance)

        ParatooCollectionId surveyId = ParatooCollectionId.fromMap(dataSet.surveyId)

        ParatooProtocolConfig config = getProtocolConfig(surveyId.protocolId)
        Map surveyData = retrieveSurveyData(surveyId, config)

        if (surveyData) {
            // If we are unable to create a site, null will be returned - assigning a null siteId is valid.
            dataSet.siteId = createSiteFromSurveyData(surveyData, collection, surveyId, project.project, config)
            dataSet.startDate = config.getStartDate(surveyData)
            dataSet.endDate = config.getEndDate(surveyData)

            createActivityFromSurveyData(surveyId, collection.orgMintedUUID, surveyData, config, project)
        }
        else {
            log.warn("Unable to retrieve survey data for: "+collection.orgMintedUUID)
            log.debug(surveyData)
        }

        Map result = projectService.update([custom:project.project.custom], project.id, false)

        result
    }

    private void createActivityFromSurveyData(ParatooCollectionId paratooSurveyId, String mintedCollectionId, Map surveyData, ParatooProtocolConfig config, ParatooProject project) {
        ActivityForm form = ActivityForm.findByExternalId(paratooSurveyId.protocolId)
        if (!form) {
            log.error("No activity form found for protocol: "+paratooSurveyId.protocolId)
        }
        else {
            Map activity = mapActivity(mintedCollectionId, paratooSurveyId, surveyData, form, config, project)
            activityService.create(activity)
        }

    }

    private static Map mapActivity(String mintedCollectionId, ParatooCollectionId surveyId, Map surveyData, ActivityForm activityForm, ParatooProtocolConfig config, ParatooProject project) {
        Map activity = [:]
        activity.projectId = project.id
        activity.startDate = config.getStartDate(surveyData)
        activity.endDate = config.getEndDate(surveyData)
        activity.type = activityForm.name
        activity.description =  activityForm.name + " - " + DateUtil.formatAsDisplayDate(surveyId.eventTime)

        Map output = [
                name: 'Unstructured',
                data: surveyData
        ]
        activity.outputs = [output]
        activity.externalIds = [new ExternalId(idType:ExternalId.IdType.MONITOR_MINTED_COLLECTION_ID, externalId: mintedCollectionId)]

        activity
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

    Map findDataSet(String userId, String orgMintedUUID) {
        List projects = findUserProjects(userId)

        Map dataSet = null
        ParatooProject project = projects?.find {
            dataSet = it.dataSets?.find { it.dataSetId == orgMintedUUID }
            dataSet
        }
        [dataSet:dataSet, project:project]
    }

    private String createSiteFromSurveyData(Map surveyData, ParatooCollection collection, ParatooCollectionId surveyId, Project project, ParatooProtocolConfig config) {
        String siteId = null
        // Create a site representing the location of the collection
        Map geoJson = config.getGeoJson(surveyData)
        if (geoJson) {
            Map siteProps = siteService.propertiesFromGeoJson(geoJson, 'upload')
            siteProps.type = Site.TYPE_SURVEY_AREA
            siteProps.publicationStatus = PublicationStatus.PUBLISHED
            siteProps.projects = [project.projectId]
            String externalId = geoJson.properties?.externalId
            if (externalId) {
                siteProps.externalIds = [new ExternalId(idType:ExternalId.IdType.MONITOR_PLOT_GUID, externalId: externalId)]
            }
            Site site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, externalId)
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
        List guids = []
        protocols.each { Map protocol ->
            String id = protocol.id
            String guid = protocol.attributes.identifier
            guids << guid
            String name = protocol.attributes.name
            ParatooProtocolConfig protocolConfig = getProtocolConfig(guid)
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
                ExternalId paratooInternalId = form.externalIds.find{it.idType == ExternalId.IdType.MONITOR_PROTOCOL_INTERNAL_ID}

                // Paratoo internal protocol ids are not stable so if we match the guid, we may need to update
                // the id as that is used in other API methods.
                if (paratooInternalId) {
                    String message = "Updating form with id: "+paratooInternalId.externalId+", guid: "+guid+", name: "+name+", new id: "+id
                    paratooInternalId.externalId = id
                    result.messages << message
                    log.info message
                }
                else {
                    String error = "Error: Missing internal id for form with id: "+id+", name: "+name
                    result.errors << error
                    log.error error
                }

            }

            List tags = protocolConfig?.tags ?: [ActivityForm.SURVEY_TAG]
            mapProtocolToActivityForm(protocol, form, tags)
            form.save()

            if (form.hasErrors()) {
                result.errors << form.errors
                log.warn "Error saving form with id: "+id+", name: "+name
            }
        }

        List allProtocolForms = ActivityForm.findAll {
            externalIds {
                idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID
            }
            status != Status.DELETED
        }

        List deletions = allProtocolForms.findAll{it.externalIds.find{it.idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID && !(it.externalId in guids)}}
        deletions.each { ActivityForm activityForm ->
            result.messages << "Form ${activityForm.name} with guid: ${activityForm.externalIds.find{it.idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID}.externalId} has been deleted"
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
        String accessToken = tokenService.getAuthToken(true)
        if (!accessToken?.startsWith('Bearer')) {
            accessToken = 'Bearer '+accessToken
        }
        Map authHeader = [(MONITOR_AUTH_HEADER):accessToken]
        Map response = webService.getJson(url, null,  authHeader, false)
        syncParatooProtocols(response?.data)
    }

    private static void mapProtocolToActivityForm(Map protocol, ActivityForm form, List tags) {
        form.name = protocol.attributes.name
        form.formVersion = protocol.attributes.version
        form.type = PARATOO_PROTOCOL_FORM_TYPE
        form.category = protocol.attributes.module
        form.external = true
        form.publicationStatus = PublicationStatus.PUBLISHED
        form.description = protocol.attributes.description
        form.tags = tags
        form.externalIds
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
                grantID:project.grantId,
                accessLevel: accessLevel,
                project:project,
                projectArea: projectAreaGeoJson,
                projectAreaSite: projectArea,
                plots: plotSelections]
        new ParatooProject(attributes)

    }

    private static Map mapParatooCollectionId(ParatooCollectionId paratooCollectionId, Project project) {
        Map dataSet = [:]
        dataSet.dataSetId = paratooCollectionId.survey_metadata.orgMintedUUID
        dataSet.protocol = paratooCollectionId.protocolId
        dataSet.grantId = project.grantId
        dataSet.collectionApp = PARATOO_APP_NAME
        dataSet.dateCreated = DateUtil.format(new Date())
        dataSet.lastUpdated = DateUtil.format(new Date())

        dataSet.progress = Activity.PLANNED
        String dataSetName = buildName(
                paratooCollectionId.protocolId,
                DateUtil.formatAsDisplayDate(paratooCollectionId.eventTime), project)
        dataSet.name = dataSetName

        dataSet
    }

    private static String buildSurveyQueryString(int start, int limit) {
        "?populate=deep&sort=updatedAt&pagination[start]=$start&pagination[limit]=$limit"
    }

    Map retrieveSurveyData(ParatooCollectionId surveyId, ParatooProtocolConfig config) {

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


    private static Map findMatchingSurvey(ParatooCollectionId surveyId, List data, ParatooProtocolConfig config) {
        data?.find { config.matches(it, surveyId) }
    }

    Map addOrUpdatePlotSelections(String userId, ParatooPlotSelectionData plotSelectionData) {

        List projects = userProjects(userId)
        if (!projects) {
            return [error:'User has no projects eligible for Monitor site data']
        }

        Map siteData = mapPlotSelection(plotSelectionData)
        // The project/s for the site will be specified by a subsequent call to /projects
        siteData.projects = []

        Site site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, siteData.externalId)
        Map result
        if (site) {
            result = siteService.update(siteData, site.siteId)
        }
        else {
            result = siteService.create(siteData)
        }

        result
    }

    private static Map mapPlotSelection(ParatooPlotSelectionData plotSelectionData) {
        Map geoJson = ParatooProtocolConfig.plotSelectionToGeoJson(plotSelectionData)
        Map site = SiteService.propertiesFromGeoJson(geoJson, 'point')
        site.projects = [] // get all projects for the user I suppose - not sure why this isn't in the payload as it's in the UI...
        site.type = Site.TYPE_SURVEY_AREA
        site.externalIds = [new ExternalId(idType:ExternalId.IdType.MONITOR_PLOT_GUID, externalId:geoJson.properties.externalId)]
        site.publicationStatus = PublicationStatus.PUBLISHED // Mark the plot as read only as it is managed by the Monitor app

        site
    }

    Map updateProjectSites(ParatooProject project, Map siteData, List<ParatooProject> userProjects) {
        if (siteData.plot_selections) {
            List siteExternalIds = siteData.plot_selections
            siteExternalIds = siteExternalIds.findAll{it} // Remove null / empty ids
            if (siteExternalIds) {
                linkProjectToSites(project, siteExternalIds, userProjects)
            }

        }
        if (siteData.project_area_type && siteData.project_area_coordinates) {
            updateProjectArea(project, siteData.project_area_type, siteData.project_area_coordinates)
        }
    }


    private static Map linkProjectToSites(ParatooProject project, List siteExternalIds, List<ParatooProject> userProjects) {
        List errors = []

        siteExternalIds.each { String siteExternalId ->

            Site site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, siteExternalId)
            if (site) {
                site.projects = site.projects ?: []
                if (!site.projects.contains(project.id)) {
                    // Validate that the user has permission to link the site to the project by checking
                    // if the user has permission on any other projects this site is linked to.
                    if (site.projects) {
                        if (!userProjects.collect{it.id}.containsAll(site.projects)) {
                            errors << "User does not have permission to link site ${site.externalId} to project ${project.id}"
                            return
                        }
                    }
                    site.projects << project.id
                    site.save()
                    if (site.hasErrors()) {
                        errors << site.errors
                    }
                }
            }
            else {
                errors << "No site exists with externalId = ${siteExternalId}"
            }
        }
        [success:!errors, error:errors]
    }

    private Map updateProjectArea(ParatooProject project, String type, List coordinates) {
        Map geometry = ParatooProtocolConfig.toGeometry(coordinates)
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
}
