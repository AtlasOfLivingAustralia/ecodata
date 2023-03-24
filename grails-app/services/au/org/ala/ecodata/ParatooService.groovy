package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooProject
import grails.converters.JSON
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j

/**
 * Implements the paratoo "org" interface.
 */
@Slf4j
class ParatooService {

    static final String PARATOO_PROTOCOL_PATH = '/api/protocols'
    static final String PARATOO_PROTOCOL_FORM_TYPE = 'Protocol'
    static final String PARTOO_SERVICE_MAPPING_KEY = 'paratoo.service_protocol_mapping'

    PermissionService permissionService
    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService

    List<ParatooProject> userProjects(String userId, boolean includeProtocols = true) {

        List<Project> projects = findUserProjects(userId)

        List paratooProjects = projects.collect { Project project ->
            ParatooProject mappedProject = mapProject(project)
            if (includeProtocols) {
                log.debug "Finding protocols for ${project.projectId} ${project.name}"
                List<ActivityForm> protocols = []
                project.findProjectServices().each { Service service ->
                    List<Integer> externalServiceForms = service.outputs.findAll{it.externalId }.collect{it.externalId}
                    externalServiceForms.each {
                        ActivityForm form = ActivityForm.findByExternalIdAndStatusNotEqual(it, Status.DELETED)
                        protocols << form
                    }
                }
                mappedProject.protocols = protocols
            }

            // TODO - include project_area and plots if required
            mappedProject
        }

        paratooProjects
    }

    private List<Project> findUserProjects(String userId) {
        List<String> projectIds = permissionService.getProjectsForUser(userId, AccessLevel.admin, AccessLevel.editor, AccessLevel.caseManager)
        Project.findAllByProjectIdInListAndStatusNotEqual(projectIds, Status.DELETED)
    }

    Map createCollection(ParatooCollection collection) {
        Project project = Project.findByProjectId(paratooCollection.projectId)
        Map dataSet = mapParatooCollection(collection)
        List dataSets = project.custom?.dataSets ?: []
        dataSets << dataSet
        projectService.update([custom:[dataSets:dataSets]])
    }

    boolean protocolCheck(String userId, String projectId, int protocolId) {
        List projects = userProjects(userId)
        ParatooProject project = projects.find{it.id == projectId}
        project?.protocols?.find{it.externalId == protocolId}
    }

    Map syncParatooProtocols() {

        Map serviceProtocolMapping = JSON.parse(settingService.getSetting(PARTOO_SERVICE_MAPPING_KEY))

        String paratooCoreUrlPrefix = grailsApplication.config.getProperty('paratoo.core.baseUrl') ?: 'http://localhost:1337'

        String url = paratooCoreUrlPrefix+PARATOO_PROTOCOL_PATH

        Map result = [errors:[], messages:[]]
        Map response = webService.getJson(url)
        response.data.each { Map protocol ->
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
                // Assign the protocol to the service.
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
                    }
                    else {
                        log.warn("Unable to find service with id ${serviceId} for protocol ${protocol.id}")
                    }

                }
            }
        }
        result

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

    private ParatooProject mapProject(Project project) {
        Map attributes = [
                id:project.projectId,
                name:project.name,
                dataSets: project.custom?.dataSets]
        new ParatooProject(attributes)

    }

}
