package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.ProjectXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.converters.JSON

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX

class ProjectController {

    def projectService, siteService, commonService, reportService, metadataService, activityService
    ElasticSearchService elasticSearchService

    static final BRIEF = 'brief'
    static final RICH = 'rich'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

	static ignore = ['action','controller','id']

    def index() {
        render "${Project.count()} sites"
    }

    def list() {
        println 'brief = ' + params.brief
        def list = projectService.list(params.brief, params.includeDeleted, params.citizenScienceOnly)
        list.sort {it.name}
        render list as JSON
    }
	
	def promoted() {
        def list = projectService.promoted()
        list.sort {it.name}
        render list as JSON
	}
	
    def get(String id) {
        def citizenScienceOnly = params.boolean('citizenScienceOnly', false)
        def includeDeleted = params.boolean('includeDeleted', false)
        def levelOfDetail = []
        if (params.brief || params.view == BRIEF) { levelOfDetail << BRIEF }
        if (params.view == RICH) { levelOfDetail << RICH }
        if (params.view == ProjectService.ALL) { levelOfDetail = ProjectService.ALL }
        if (params.view == ProjectService.OUTPUT_SUMMARY) {levelOfDetail = ProjectService.OUTPUT_SUMMARY}
        if (!id) {
            def list = projectService.list(levelOfDetail, includeDeleted, citizenScienceOnly)
            list.sort {it.name}
            asJson([list: list])
        } else {
            def p = Project.findByProjectId(id)
            if (p) {

                withFormat {
                    json {
                        asJson projectService.toMap(p, levelOfDetail, includeDeleted)
                    }
                    xlsx {
                        asXlsx projectService.toMap(p, 'all')  // Probably should only support one level of detail?
                    }
                    shp {
                        asShapefile p // Make sure sites are included
                    }
                }


            } else {
                render (status: 404, text: 'No such id')
            }
        }
    }

    def asXlsx(project) {

        XlsExporter exporter = new XlsExporter(URLEncoder.encode(project.name, 'UTF-8'))
        exporter.setResponseHeaders(response)
        ProjectXlsExporter projectExporter = new ProjectXlsExporter(exporter, metadataService)
        projectExporter.export(project)
        exporter.sizeColumns()

        exporter.save(response.outputStream)
    }

    def asShapefile(project) {
        if (siteService.findAllForProjectId(project.projectId)) {
            def name = 'projectSites'
            response.setContentType("application/zip")
            response.setHeader("Content-disposition", "filename=${name}.zip")
            reportService.exportShapeFile([project.projectId], name, response.outputStream)
            response.outputStream.flush()
        }
        else {
            render status: 404, text: 'No sites are configured for this project'
        }
    }


    def delete(String id) {
        Project project = Project.findByProjectId(id)
        if (project) {
            boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()
            Map result = projectService.delete(id, destroy)
            if (!result.error) {
                render (status: 200, text: 'deleted')
            }
            else {
                render (status:400, text:result.error)
            }

        } else {
            render (status: 404, text: 'No such id')
        }
    }

    @RequireApiKey
    def resurrect(String id) {
        def p = Project.findByProjectId(id)
        if (p) {
            p.status = 'active'
            p.save(flush: true)
            render (status: 200, text: 'raised from the dead')
        } else {
            render (status: 404, text: 'No such id')
        }
    }

    def deleteSites(String id){
        def status = siteService.deleteSitesFromProject(id)
        if(status.status == 'ok'){
            render (status: 200, text: 'raised from the dead')
        } else {
            render (status: 500, text: 'No such id')
        }
    }

    /**
     * Update the site for project.
     * @param id
     * @return
     */
    @RequireApiKey
    def updateSites(String id){
        log.debug("Updating the sites for projectID : " + id)
        def props = request.JSON
        log.debug props
        def allCurrentSites = []
        Site.findAllByProjects(id).each{
          allCurrentSites << it.siteId
        }
        //sites to remove...

        log.debug("Current sites: " + allCurrentSites)

        def doNothings = allCurrentSites.intersect(props.sites)

        def toAdd = props.sites.minus(doNothings)
        def toRemove = allCurrentSites.minus(doNothings)

        //sites to remove
        toAdd.each { siteId -> siteService.addProject(siteId, id) }
        toRemove.each { siteId -> siteService.removeProject(siteId, id) }
        def response = [status: 200]
        asJson response
    }

    /**
     * Create or update a project.
     *
     * @param id - identifies the resource
     * @return
     */
    @RequireApiKey
    def update(String id) {
        def props = request.JSON
        log.debug props
        def result
        def message
        if (id) {
            result = projectService.update(props,id)
            message = [message: 'updated']
        } else {
            result = projectService.create(props)
            message = [message: 'created', projectId: result.projectId]
        }
        if (result.status == 'ok') {
            setResponseHeadersForProjectId(response, result.projectId)
            asJson(message)
        } else {
            log.error result.error
            render status:400, text: result.error
        }
    }

    @RequireApiKey
    def downloadProjectData() {

        def p = Project.findByProjectId(params.id)

        if (!p) {
            render (status: 404, text: 'No such id')
        } else {
            Set ids = new HashSet()
            ids << params.id;

            withFormat {
                json {
                    List projects = ids.collect{projectService.get(it,ProjectService.ALL)}
                    render projects as JSON
                }
                xlsx {
                    XlsExporter exporter = new XlsExporter("results")
                    exporter.setResponseHeaders(response)
                    ProjectXlsExporter projectExporter = new ProjectXlsExporter(exporter, metadataService)

                    List projects = ids.collect{projectService.get(it,ProjectService.ALL)}
                    projectExporter.exportAll(projects)
                    exporter.sizeColumns()

                    exporter.save(response.outputStream)
                }
            }
        }
    }

    def projectMetrics(String id) {

        // TODO this is temporarily hardcoded, but we can maybe define a meta model for reporting
        // Need to add targets to this also.
        def p = Project.findByProjectId(id)

        if (p) {
            render projectService.projectMetrics(id) as JSON

        } else {
            render (status: 404, text: 'No such id')
        }
    }

    /**
     * Request body should be JSON formatted of the form:
     * {
     *     "property1":value1,
     *     "property2":value2,
     *     etc
     * }
     * where valueN may be a primitive type or array.
     * The criteria are ANDed together.
     * If a property is supplied that isn't a property of the project, it will not cause
     * an error, but no results will be returned.  (this is an effect of mongo allowing
     * a dynamic schema)
     *
     * @return a list of the projects that match the supplied criteria
     */
    @RequireApiKey
    def search() {
        def searchCriteria = request.JSON

        def view = searchCriteria.remove('view') ?: ProjectService.BRIEF
        def projectList = projectService.search(searchCriteria, view)
        asJson projects:projectList
    }

    @RequireApiKey
    def findByName() {
        if (!params.projectName) {
            render status:400, text: "projectName is a required parameter"
        } else {
            render projectService.findByName(params.projectName) as JSON
        }
    }

    @PreAuthorise
    def eSearch() {
        String error = ""
        if (params.max && !params.max.isNumber()) {
            error = "Invalid max parameter."
        } else if (params.offset && !params.offset.isNumber()) {
            error = "Invalid offset parameter."
        } else if (params.sort) {
            List options = ['nameSort', '_score', 'organisationSort']
            String found = options.find { it == params.sort }
            error = !found ? 'Invalid sort parameter (Accepted values: nameSort, _score, organisationSort ).' : ''
        } else if(params.order) {
            List options = ['ASC', 'DESC']
            String found = options.find { it == params.sort }
            error = !found ? 'Invalid order parameter (Accepted values: ASC, DESC ).' : ''
        }
        if (!error) {
            Map params = buildParams(params)
            def result = elasticSearchService.search(params.query, params, HOMEPAGE_INDEX)

            response.setContentType('application/json; charset="UTF-8"')
            render result
        } else {
            render status: 400, text: error
        }
    }

    def importProjectsFromSciStarter(){
        List projects = projectService.importProjectsFromSciStarter()?:[]
        render(text: [count: projects.size(), projects: projects] as JSON, contentType: 'application/json');
    }

    private Map buildParams(Map params){
        Map values = [:]
        values.sort = params.sort ?: 'nameSort'
        values.max = params.max ?: 10
        values.skipDefaultFilters = false
        values.offset = params?.offset ?: 0
        values.query = "docType:project AND (isCitizenScience:true)" + (params.query ? (" AND " + params.query) : "")
        values.order = params.order ?: 'ASC'

        values
    }

    private def setResponseHeadersForProjectId(response, projectId){
        response.addHeader("content-location", grailsApplication.config.grails.serverURL + "/project/" + projectId)
        response.addHeader("location", grailsApplication.config.grails.serverURL + "/project/" +  projectId)
        response.addHeader("entityId", projectId)
    }

}
