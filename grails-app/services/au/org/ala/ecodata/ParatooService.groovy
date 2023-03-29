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

    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService
    ProjectService projectService
    SiteService siteService

    List<ParatooProject> userProjects(String userId, boolean includeProtocols = true) {

        List<ParatooProject> projects = findUserProjects(userId)

        projects.each { ParatooProject project ->
            if (includeProtocols) {
                log.debug "Finding protocols for ${project.id} ${project.name}"
                List<ActivityForm> protocols = []
                project.findProjectServices().each { Service service ->
                    protocols += findServiceProtocols(service, protocols)
                }
                project.protocols = protocols
            }

            // TODO - include project_area and plots if required
            project
        }

        projects
    }

    private List findServiceProtocols(Service service) {
        List<ActivityForm> protocols
        List<Integer> externalServiceForms = service.outputs.findAll { it.externalId }.collect { it.externalId }
        externalServiceForms.each {
            ActivityForm form = ActivityForm.findByExternalIdAndStatusNotEqual(it, Status.DELETED)
            protocols << form
        }
        protocols
    }

    private List<ParatooProject> findUserProjects(String userId) {
        List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, Project.class.name, Status.DELETED)
        List projects = Project.findAllByProjectIdInListAndStatusNotEqual(permissions.collect{it.entityId}, Status.DELETED)
        List paratooProjects = projects.collect { Project project ->
            List<Site> sites = siteService.sitesForProject(project.projectId)
            UserPermission permission = permissions.find{it.entityId == project.projectId}
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

    boolean protocolCheck(String userId, String projectId, int protocolId) {
        List projects = userProjects(userId)
        ParatooProject project = projects.find{it.id == projectId}
        project?.protocols?.find{it.externalId == protocolId}
    }

    ParatooProject findDataSet(String userId, String collectionId) {
        List projects = userProjects(userId, false)

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

        List serviceIds = serviceProtocolMapping[(protocol.module)] ?: serviceProtocolMapping[(protocol.id as String)]
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
        String paratooCoreUrlPrefix = grailsApplication.config.getProperty('paratoo.core.baseUrl') ?: 'http://localhost:1337'
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
        Map attributes = [
                id:project.projectId,
                name:project.name,
                accessLevel: permission.accessLevel,
                project:project,
                projectArea: sites.find{it.type == Site.TYPE_PROJECT_AREA},
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
