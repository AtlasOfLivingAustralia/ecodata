package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooProtocolId
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
    static final String PARTOO_PROTOCOLS_KEY = 'paratoo.protocols'
    static final String PROGRAM_CONFIG_PARATOO_ITEM = 'supportsParatoo'
    static final String PARATOO_APP_NAME = "Monitor"
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


        // If the permission has been set as a favourite then delegate to the Hub permission
        // so that "readOnly" access for a hub is supported, and we don't return projects that a user
        // has only marked as starred without having hub level permissions
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
            config?.get(PROGRAM_CONFIG_PARATOO_ITEM) && projectAccessLevels[project.projectId]
        }

        List paratooProjects = projects.collect { Project project ->
            List<Site> sites = siteService.sitesForProject(project.projectId)
            AccessLevel accessLevel = projectAccessLevels[project.projectId]
            mapProject(project, accessLevel, sites)
        }
        paratooProjects

    }

    Map mintCollectionId(ParatooCollectionId paratooCollectionId) {
        String projectId = paratooCollectionId.projectId
        Project project = Project.findByProjectId(projectId)
        Map dataSet = mapParatooCollectionId(paratooCollectionId, project)
        dataSet.progress = Activity.STARTED

        String dataSetName = buildName(paratooCollectionId.protocol, paratooCollectionId.surveyId, project)

        dataSet.name = dataSetName

        if (!project.custom) {
            project.custom = [:]
        }
        if (!project.custom.dataSets) {
            project.custom.dataSets = []
        }
        project.custom.dataSets << dataSet
        Map result = projectService.update([custom:project.custom], projectId, false)

        if (!result.error) {
            result.orgMintedIdentifier = dataSet.dataSetId
        }
        result
    }

    private static String buildName(ParatooProtocolId protocolId, ParatooSurveyId surveyId, Project project) {
        ActivityForm protocolForm = ActivityForm.findByExternalIdAndStatusNotEqual(protocolId.id, Status.DELETED)
        String dataSetName = protocolForm?.name + " - " + surveyId.timeAsDisplayDate() + " (" + project.name + ")"
        dataSetName
    }

    Map submitCollection(ParatooCollection collection) {
        String projectId = collection.projectId
        Project project = Project.findByProjectId(projectId)
        Map dataSet = project.custom?.dataSets?.find{it.dataSetId == collection.mintedCollectionId}

        if (!dataSet) {
            throw new RuntimeException("Unable to find data set with id: "+collection.mintedCollectionId)
        }

        dataSet.activitiesEndDate = collection.eventTime

        ParatooSurveyId surveyId = ParatooSurveyId.fromMap(dataSet.surveyId)
        Map surveyData = retrieveSurveyData(surveyId, collection)

        // If we are unable to create a site, null will be returned - assigning a null siteId is valid.
        dataSet.siteId = createSiteFromSurveyData(surveyData, collection, surveyId, project)

        // Find the dates in the survey data and update the data set accordingly


        Map result = projectService.update([custom:[dataSets:project.custom.dataSets]], projectId, false)

        result
    }

    boolean protocolReadCheck(String userId, String projectId, int protocolId) {
        protocolCheck(userId, projectId, protocolId, true)
    }

    boolean protocolWriteCheck(String userId, String projectId, int protocolId) {
        protocolCheck(userId, projectId, protocolId, false)
    }

    private boolean protocolCheck(String userId, String projectId, int protocolId, boolean read) {
        List projects = userProjects(userId)
        ParatooProject project = projects.find{it.id == projectId}
        boolean protocol = project?.protocols?.find{it.externalId == protocolId}
        int minimumAccess = read ? AccessLevel.projectParticipant.code : AccessLevel.editor.code
        protocol && project.accessLevel.code >= minimumAccess
    }

    ParatooProject findDataSet(String userId, String collectionId) {
        List projects = findUserProjects(userId)

        Project projectWithMatchingDataSet = projects?.find {
            it.dataSets?.find { it.dataSetId == collectionId }
        }
        projectWithMatchingDataSet
    }

    private String createSiteFromSurveyData(Map surveyData, ParatooCollection collection, ParatooSurveyId surveyId, Project project) {
        // Create a site representing the location of the collection
        Map geoJson = extractSpatialData(surveyData)

        String siteName = buildName(collection.protocol, surveyId, project)

        Map result = siteService.create([extent:[geometry:geoJson], name: siteName, type: 'Survey', publicatonStatus: PublicationStatus.PUBLISHED, projects: [project.projectId]])
        if (result.error) {  // Don't treat this as a fatal error for the purposes of responding to the paratoo request
            log.error("Error creating a site for survey "+collection.mintedCollectionId+", project "+project.projectId+": "+result.error)
        }
        result.siteId
    }

    private Map syncParatooProtocols(List<Map> protocols) {

        Map result = [errors:[], messages:[]]
        protocols.each { Map protocol ->
            ActivityForm form = ActivityForm.findByExternalIdAndStatusNotEqual(protocol.id, Status.DELETED)
            if (!form) {
                form = new ActivityForm(externalId: protocol.id)
                String message = "Creating form with id: "+protocol.id+", name: "+protocol.attributes?.name
                result.messages << message
                log.info message
            }
            else {
                String message = "Updating form with id: "+protocol.id+", name: "+protocol.attributes?.name
                result.messages << message
                log.info message
            }
            mapProtocolToActivityForm(protocol, form)
            form.save()

            if (form.hasErrors()) {
                result.errors << form.errors
                log.warn "Error saving form with id: "+protocol.id+", name: "+protocol.attributes?.name
            }
        }
        result

    }

    /** This is a backup method in case the protocols aren't available online */
    Map syncProtocolsFromSettings() {
        List protocols = JSON.parse(settingService.getSetting(PARTOO_PROTOCOLS_KEY))
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

        Map attributes = [
                id:project.projectId,
                name:project.name,
                accessLevel: accessLevel,
                project:project,
                projectArea: projectAreaGeoJson,
                plots: sites.findAll{it.type == Site.TYPE_WORKS_AREA}]
        new ParatooProject(attributes)

    }

    private static Map mapParatooCollectionId(ParatooCollectionId collectionId, Project project) {
        Map dataSet = [:]
        dataSet.dataSetId = Identifiers.getNew(true, null)
        dataSet.surveyId = collectionId.surveyId.toMap() // No codec to save this to mongo
        dataSet.grantId = project.grantId
        dataSet.activitesStartDate = DateUtil.format(collectionId.surveyId.time)
        dataSet.collectionApp = PARATOO_APP_NAME
        dataSet
    }

    Map retrieveSurveyData(ParatooSurveyId surveyId, ParatooCollection collection) {

        // We might be able to replace this call with surveyId.surveyType
        String url = paratooBaseUrl+PARATOO_PROTOCOL_PATH+'/'+collection.protocol.id
        Map response = webService.getJson(url, null,  null, false)

        String apiEndpoint = response?.data?.attributes?.endpointPrefix
        if (!apiEndpoint.endsWith('s')) {
            apiEndpoint += 's'
        } // strapi makes the endpoint plural sometimes?


        String accessToken = tokenService.getAuthToken(true)

        if (!accessToken) {
            throw new RuntimeException("Unable to get access token")
        }
        int start = 0
        int limit = 10

        String query = "?populate=deep&sort=updatedAt&start=$start&limit=$limit&auth=$accessToken"
        url = paratooBaseUrl+apiEndpoint+query
        response = webService.getJson(url, null,  null, false)
        int total = response.meta?.pagination?.total ?: 0

        Map survey = null
        while (!survey && start < total) {
            List data = response?.data

            survey = findMatchingSurvey(surveyId, data)
            start += limit

            if (!survey) {
                query = "?populate=deep&sort=updatedAt&start=$start&limit=$limit&auth=$accessToken"
                response = webService.getJson(apiEndpoint+query, null,  null, false)
            }
        }

        survey
    }

    private static Map findMatchingSurvey(ParatooSurveyId surveyId, List data) {
        data?.find {
            Map tmpSurveyId = it.attributes?.surveyId
            tmpSurveyId.surveyType == surveyId.surveyType &&
            DateUtil.format(DateUtil.parseWithMilliseconds(tmpSurveyId.time)) == surveyId.timeAsISOString() &&
            tmpSurveyId.randNum == surveyId.randNum
        }
    }

    private static Map extractSpatialData(Map survey) {
        if (!survey) {
            return null
        }
        if (survey.attributes) {
            survey = survey.attributes
        }

        Map siteData = null
        if (survey.plot_visit) {
            siteData = extractSiteDataFromPlotVisit(survey)
        }
        // else { ... } - other survey types can embed spatial data in different ways
        siteData
    }

    private static Map extractSiteDataFromPlotVisit(Map survey) {
        Map plotLayout = survey.plot_visit.data?.attributes?.plot_layout?.data?.attributes

        Map plotGeoJson = toGeoJson(plotLayout.plot_points)
        Map faunaPlotGeoJson = toGeoJson(plotLayout.fauna_plot_point)

        // TODO maybe turn this into a feature with properties to distinguish the fauna plot?
        // Or a multi-polygon?

        plotGeoJson
    }

    static Map toGeoJson(List points) {
        List coords = points?.findAll { !exclude(it) }.collect {
            [it.lng, it.lat]
        }
        Map plotGeometry = coords ? [
                type       : 'Polygon',
                coordinates: [closePolygonIfRequired(coords)]
        ] : null

        plotGeometry
    }

    static List closePolygonIfRequired(List points) {
        if (points[0][0] != points[-1][0] && points[0][1] != points[-1][1]) {
            points << points[0]
        }
        points
    }

    static boolean exclude(Map point) {
        point.name?.data?.attributes?.symbol == "C" // The plot layout has a centre point that we don't want
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
