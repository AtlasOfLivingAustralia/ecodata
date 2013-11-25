package au.org.ala.ecodata
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.report.ProcessingReport
import grails.converters.JSON
import org.springframework.web.util.UriUtils

/**
 *  Provides a single interface for external (as in not other ALA apps) web service clients.
 *  Not really sure if this is a good idea or should instead just be incorporated into the other clients via a filter
 *  or not but it will do for now.
 *
 *  Handles ids & authentication differently & performs more intrusive validation.
 *  Also provides an interface at a different level of granularity than the other controllers.
 */
class ExternalController {

    def grailsApplication, projectService, activityService, metadataService

    def validateSchema() {

        def results = [:]
        metadataService.activitiesModel().outputs.each {

            JsonSchemaFactory factory = JsonSchemaFactory.byDefault()

            def encodedName = UriUtils.encodePathSegment(it.name, 'UTF-8')
            println encodedName
            JsonSchema schema = factory.getJsonSchema(grailsApplication.config.grails.serverURL+'/ws/documentation/draft/output/'+encodedName)
            def payload = JsonLoader.fromString("{}")

            ProcessingReport report = schema.validateUnchecked(payload)
            if (!report.isSuccess()) {
                println report
            }
            results << [(it.name):jacksonToJSON(report)]

        }
        render results as JSON
    }

    def projectActivities() {

        def payload
        try {

            String payloadText = request.inputStream.getText(request.characterEncoding?:'ISO-8859-1')
            log.info("ExternalController::projectPlan with payload: ${payloadText} from: ${request.getRemoteAddr()}")

            payload = JsonLoader.fromString(payloadText)
        }
        catch (Exception e) {
            render (status:400, contentType: 'text/json', text: [message:'Unparsable input - the request body is not valid JSON'] as JSON)
            return
        }

        try {
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault()
            JsonSchema schema = factory.getJsonSchema(grailsApplication.config.grails.serverURL+'/ws/documentation/v1/project')

            ProcessingReport report = schema.validate(payload)

            if (!report.isSuccess()) {
                def messages = report.iterator().collect{messageToJSON(it)}
                def result = [success:report.isSuccess(), message:messages]
                render result as JSON
                return
            }

            def projectJson = jacksonToJSON(payload)

            Project project = findProject(projectJson.projectId)
            if (!project) {
                render (status:400, contentType: 'text/json', text: [message:"Invalid project id: ${projectJson.projectId.value}"] as JSON)
                return
            }

            // Do something with security here... check API key, get the projectId from the payload.

            // Update the projectId
            projectJson.projectId = project.projectId

            def activities = projectJson.remove('activities')
            projectService.update(projectJson, project.projectId)

            // What are the semantics we should expect here?  Should be be deleting all existing activities then
            // creating new ones?  Should we be requiring ids for each activity?
            activities.each {
                it << [projectId:project.projectId]
                activityService.create(it)
            }

            def result = [status:200, message:'Project plan updated']
            render result as JSON


        }
        catch (Exception e) {
            e.printStackTrace()
            render (status:500, message:e.getMessage())
        }

    }

    /**
     * The asJson returns a Jackson JSON object which doesn't have a type converter for "as JSON" so
     * we are turning it into a String then parsing it.  Probably can register a type converter for Jackson...
     */
    def messageToJSON(message) {
        return jacksonToJSON(message.asJson())
    }

    def jacksonToJSON(jackson) {
        return JSON.parse(jackson.toString())
    }

    /**
     * The project id has a type and value field that determines how to lookup the project.  By the time we
     * get here the schema has been validated, so we know it is one of the three allowed types.
     * @param projectId specifies the type and value of the key used to identify the project.
     */
    def findProject(projectId) {

        switch (projectId.type) {
            case 'grantId':
                return Project.findByGrantId(projectId.value)

            case 'externalId':
                return Project.findByExternalProjectId(projectId.value)

            case 'internalId':
                return Project.findByProjectId(projectId.value)
        }
    }

}
