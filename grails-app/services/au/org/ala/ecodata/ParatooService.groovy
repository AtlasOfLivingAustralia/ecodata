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
    static final String PARTOO_SERVICE_MAPPING_KEY = 'paratoo.service_protocol_mapping'
    static final String PARTOO_PROTOCOLS_KEY = 'paratoo.protocols'
    static final String PROGRAM_CONFIG_PARATOO_ITEM = 'supportsParatoo'

    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService
    ProjectService projectService
    SiteService siteService

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
    List<ParatooProject> userProjects(String userId, boolean includeProtocols = true) {

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
            protocols += findProtocolsByCategories(monitoringProtocolCategories)
        }
        // Disabling the service -> protocol mapping for now as they are directly assignable via the
        // MERI plan
        //
        //        project.findProjectServices().each { Service service ->
        //            protocols += findServiceProtocols(service)
        //        }
        // TODO a future implementation could also find ProjectActivites configured with
        // Paratoo activity types to support BioCollect
        protocols
    }

    private List findServiceProtocols(Service service) {
        List<ActivityForm> protocols = []
        List<Integer> externalServiceForms = service.outputs.findAll { it.externalId }.collect { it.externalId }
        externalServiceForms.each {
            ActivityForm form = ActivityForm.findByExternalIdAndStatusNotEqual(it, Status.DELETED)
            protocols << form
        }
        protocols
    }

    private List findProtocolsByCategories(List categories) {
        List<ActivityForm> forms = ActivityForm.findAllByCategoryInListAndExternalAndStatusNotEqual(categories, true, Status.DELETED)
        forms
    }

    private List<ParatooProject> findUserProjects(String userId) {
        List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, Project.class.name, Status.DELETED)
        List projects = Project.findAllByProjectIdInListAndStatus(permissions.collect{it.entityId}, Status.ACTIVE)

        // Filter projects that aren't in a program configured to support paratoo
        projects = projects.findAll {
            if (!it.programId) {
                return false
            }
            Program program = Program.findByProgramId(it.programId)
            Map config = program.getInheritedConfig()
            config?.get(PROGRAM_CONFIG_PARATOO_ITEM)
        }

        List paratooProjects = projects.collect { Project project ->
            List<Site> sites = siteService.sitesForProject(project.projectId)
            UserPermission permission = permissions.find{it.entityId == project.projectId}
            // If the permission has been set as a favourite then delegate to the Hub permission
            // so that "readOnly" access for a hub is supported.
            if (permission.accessLevel == AccessLevel.starred) {
                Hub hub = Hub.findByHubId(project.getHubId())
                permission = permissions.find{it.entityId == hub.hubId}
            }
            mapProject(project, permission, sites)
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

        Map serviceProtocolMapping = JSON.parse(settingService.getSetting(PARTOO_SERVICE_MAPPING_KEY))

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
            else {
                createOrUpdateProtocolServiceMapping(serviceProtocolMapping, protocol)
            }
        }
        result

    }

    private void createOrUpdateProtocolServiceMapping(Map serviceProtocolMapping, Map protocol) {

        List serviceIds = serviceProtocolMapping[(protocol.attributes.module)] ?: serviceProtocolMapping[(protocol.id as String)]
        println serviceIds
        for (int serviceId : serviceIds) {

            Service service = Service.findByLegacyId(serviceId)
            if (service) {
                ServiceForm serviceForm = service.outputs.find { it.externalId == protocol.id }
                if (!serviceForm) {
                    serviceForm = new ServiceForm(formName: PARATOO_PROTOCOL_FORM_TYPE, externalId: protocol.id)
                    log.info "Attaching protocol ${protocol.id}, name:${protocol.attributes?.name} to Service: ${service.name}"
                    service.outputs << serviceForm
                    service.markDirty('outputs')
                    service.save()

                    if (service.hasErrors()) {
                        log.warn("Error saving service ${service.name}")
                        log.warn service.errors
                    }
                } else {
                    log.info "Protocol ${protocol.id}, name:${protocol.attributes?.name} already attached to Service: ${service.name}. No action required"
                }
            } else {
                log.warn("Unable to find service with id ${serviceId} for protocol ${protocol.id}")
            }
        }
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

    private void mapProtocolToActivityForm(Map protocol, ActivityForm form) {
        form.name = protocol.attributes.name
        form.formVersion = protocol.attributes.version
        form.type = PARATOO_PROTOCOL_FORM_TYPE
        form.category = protocol.attributes.module
        form.external = true
        form.publicationStatus = PublicationStatus.PUBLISHED
        form.description = protocol.attributes.description
    }

    private ParatooProject mapProject(Project project, UserPermission permission, List<Site> sites) {
        Site projectArea = sites.find{it.type == Site.TYPE_PROJECT_AREA}
        Map projectAreaGeoJson = null
        if (projectArea) {
            projectAreaGeoJson = siteService.geometryAsGeoJson(projectArea)
        }

        Map attributes = [
                id:project.projectId,
                name:project.name,
                accessLevel: permission.accessLevel,
                project:project,
                projectArea: projectAreaGeoJson,
                plots: sites.findAll{it.type == Site.TYPE_WORKS_AREA}]
        new ParatooProject(attributes)

    }

    private Map mapParatooCollection(ParatooCollection collection, Project project) {
        Map dataSet = [:]
        dataSet.dataSetId = collection.mintedCollectionId
        dataSet.grantId = project.grantId

        dataSet
    }

}
