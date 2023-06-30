package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooProject
import grails.converters.JSON
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j

/**
 * Supports the implementation of the paratoo "org" interface
 */
@Slf4j
class ParatooService {

    static final String PARATOO_PROTOCOL_PATH = '/api/protocols'
    static final String PARATOO_PROTOCOL_FORM_TYPE = 'Protocol'
    static final String PARTOO_PROTOCOLS_KEY = 'paratoo.protocols'
    static final String PROGRAM_CONFIG_PARATOO_ITEM = 'supportsParatoo'

    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService
    ProjectService projectService
    SiteService siteService
    PermissionService permissionService

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
            protocols += findProtocolsByCategories(monitoringProtocolCategories)
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
        Map projectAccessLevel = permissions?.collectEntries { UserPermission permission ->
            String projectId = permission.entityId
            if (permission.accessLevel == AccessLevel.starred) {
                permission = permissionService.findParentPermission(permission)
            }
            // Return a Map of projectId to accessLevel
            [(projectId):permission?.accessLevel]
        }

        List projects = Project.findAllByProjectIdInListAndStatus(new ArrayList(projectAccessLevel.keySet()), Status.ACTIVE)

        // Filter projects that aren't in a program configured to support paratoo or don't have permission
        projects = projects.findAll { Project project ->
            if (!project.programId) {
                return false
            }

            Program program = Program.findByProgramId(project.programId)
            Map config = program.getInheritedConfig()
            config?.get(PROGRAM_CONFIG_PARATOO_ITEM) && projectAccessLevel[project.projectId]
        }

        List paratooProjects = projects.collect { Project project ->
            List<Site> sites = siteService.sitesForProject(project.projectId)
            AccessLevel accessLevel = projectAccessLevel[project.projectId]
            mapProject(project, accessLevel, sites)
        }
        paratooProjects

    }

    Map createCollection(ParatooCollection collection) {
        Project project = Project.findByProjectId(collection.projectId)
        Map dataSet = mapParatooCollection(collection, project)
        List dataSets = project.custom?.dataSets ?: []
        dataSets << dataSet
        projectService.update([custom:[dataSets:dataSets]], collection.projectId, false)
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

    Map syncProtocolsFromParatoo() {
        String paratooCoreUrlPrefix = grailsApplication.config.getProperty('paratoo.core.baseUrl')
        String url = paratooCoreUrlPrefix+PARATOO_PROTOCOL_PATH
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

    private static Map mapParatooCollection(ParatooCollection collection, Project project) {
        Map dataSet = [:]
        dataSet.dataSetId = collection.mintedCollectionId
        dataSet.grantId = project.grantId

        dataSet
    }

}
