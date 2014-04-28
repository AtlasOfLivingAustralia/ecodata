package au.org.ala.ecodata
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.report.ProcessingReport
import grails.converters.JSON
/**
 *  Provides a single interface for external (as in not other ALA apps) web service clients.
 *  Not really sure if this is a good idea or should instead just be incorporated into the other clients via a filter
 *  or not but it will do for now.
 *
 *  Handles ids & authentication differently & performs more intrusive validation.
 *  Also provides an interface at a different level of granularity than the other controllers.
 */
class ExternalController {

    def grailsApplication, projectService, activityService, metadataService, cacheService

    /** Temporary IP based security */
    def beforeInterceptor = [action:this.&applyWhiteList]

    /** Checks the IP of the client against a white list and returns true if the request is allowed. */
    private applyWhiteList() {

        def whiteList = buildWhiteList()
        def clientIp = getClientIP(request)
        def allowed = whiteList.contains(clientIp)

        if (!allowed) {
            log.warn("Rejected request from ${clientIp}, whitelist=${whiteList}")
        }
        else {
            log.info("Allowed request from ${clientIp}")

        }

        return allowed
    }

    private buildWhiteList() {
        def whiteList = ['127.0.0.1'] // allow calls from localhost to make testing easier
        def config = grailsApplication.config.app.api.whiteList
        if (config) {
            whiteList.addAll(config.split(',').collect({it.trim()}))
        }
        whiteList

    }

    private getClientIP(request) {

        // External requests to ecodata are proxied by Apache, which uses X-Forwarded-For to identify the original IP.
        def ip = request.getHeader("X-Forwarded-For")
        if (!ip) {
            ip = request.getRemoteHost()
        }

        return ip

    }

    def validateSchema() {

        def urlBuilder = new SchemaUrlBuilder(grailsApplication.config, metadataService)
        def results = [:]
        metadataService.activitiesModel().outputs.each {

            JsonSchemaFactory factory = JsonSchemaFactory.byDefault()

            def url = urlBuilder.outputSchemaUrl(it.name)

            JsonSchema schema = factory.getJsonSchema(url)
            def payload = JsonLoader.fromString("{}")

            ProcessingReport report = schema.validateUnchecked(payload)
            if (!report.isSuccess()) {
                println report
            }
            def messages = report.iterator().collect{messageToJSON(it)}
            def result = [success:report.isSuccess(), message:messages]
            results << [(it.name):result]

        }
        render results as JSON
    }

    def projectSites() {

        if (!params.type || !params.id) {
            render (status:400, contentType: 'text/json', text: [message:"type and id are mandatory parameters"] as JSON)
            return
        }
        def projectId = [type:params.type, id:params.id]

        def project
        try {
            project = findProject(projectId)
        }
        catch (Exception e){
            render (status:400, contentType: 'text/json', text: [message:"Grant ID ${projectId.id} is not unique"] as JSON)
            return
        }
        if (!project) {
            render (status:404, contentType: 'text/json', text: [message:"Can't find project with id: ${projectId.id}"] as JSON)
            return
        }



        def all =  projectService.toMap(project)
        def sites = []
        all.sites.each {
            sites << [siteId:it.siteId, name:it.name, description:it.description, extent:it.extent, ]
        }
        def projectDetails = [projectId:all.projectId, grantId:all.grantId, externalId:all.externalId, sites:sites]

        render projectDetails as JSON
    }

    def projectDetails() {
        if (!params.type || !params.id) {
            render (status:400, contentType: 'text/json', text: [message:"type and id are mandatory parameters"] as JSON)
            return
        }
        def projectId = [type:params.type, id:params.id]

        def project
        try {
            project = findProject(projectId)
        }
        catch (Exception e){
            render (status:400, contentType: 'text/json', text: [message:"Grant ID ${projectId.id} is not unique"] as JSON)
            return
        }
        if (!project) {
            render (status:404, contentType: 'text/json', text: [message:"Can't find project with id: ${projectId.id}"] as JSON)
            return
        }



        def all =  projectService.toMap(project)
        def sites = []
        all.sites.each {
            sites << [siteId:it.siteId, name:it.name, description:it.description, extent:it.extent, ]
        }
        all.activities = activityService.findAllForProjectId(project.projectId)
        def activities = []
        all.activities.each {
            def activity = [activityId:it.activityId, type:it.type, description: it.description, siteId: it.siteId,
                    plannedStartDate: it.plannedStartDate, plannedEndDate: it.plannedEndDate, progress:it.progress]
            if (it.startDate) {
                activity.startDate = it.startDate
            }
            if (it.actualEndDate) {
                activity.endDate = it.endDate
            }
            if (it.outputs) {
                def outputs = []
                it.outputs.each { output ->
                    outputs << [name:output.name, outputId:output.outputId, data:output.data]
                }
                activity.outputs = outputs
            }
            activities << activity
        }
        def projectDetails = [projectId:all.projectId, grantId:all.grantId, externalId:all.externalId, sites:sites, activities:activities]

        render projectDetails as JSON
    }

    private def projectActivitiesSchema() {

        return cacheService.get('projectActivitiesSchema',{
            def urlBuilder = new SchemaUrlBuilder(grailsApplication.config, metadataService)
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault()
            factory.getJsonSchema(urlBuilder.projectActivitiesSchemaUrl())
        })

    }

    def projectActivities() {

        def payload
        try {

            String payloadText = request.inputStream.getText('UTF-8')
            log.info("ExternalController::projectPlan with payload: ${payloadText} from: ${request.getRemoteAddr()}")

            payload = JsonLoader.fromString(payloadText)
        }
        catch (Exception e) {
            render (status:400, contentType: 'text/json', text: [message:'Unparsable input - the request body is not valid JSON'] as JSON)
            return
        }

        try {
            JsonSchema schema = projectActivitiesSchema()

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
                if (it.activityId) {
                    activityService.update(it, it.activityId)
                }
                else {
                    activityService.create(it)
                }
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
     * The project id has a type and id field that determines how to lookup the project.  By the time we
     * get here the schema has been validated, so we know it is one of the three allowed types.
     * @param projectId specifies the type and value of the key used to identify the project.
     */
    def findProject(projectId) {

        switch (projectId.type) {
            case 'grantId':
                def projects = Project.findAllByGrantId(projectId.id)
                if (projects.size() > 1) {
                    throw new RuntimeException("Grant ID is not unique")
                }
                else if (!projects) {
                    return null
                }
                return projects[0]
            case 'externalId':
                return Project.findByExternalId(projectId.id)

            case 'guid':
                return Project.findByProjectId(projectId.id)
        }
    }

}
