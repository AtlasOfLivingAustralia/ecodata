package au.org.ala.ecodata

import au.org.ala.web.AlaSecured
import grails.converters.JSON
import grails.util.Environment
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.multipart.MultipartFile

import java.text.SimpleDateFormat

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX
import static au.org.ala.ecodata.Status.DELETED
import static groovyx.gpars.actor.Actors.actor

class AdminController {

    def outputService, siteService, projectService, authService,
        collectoryService, organisationService,
        commonService, cacheService, metadataService, elasticSearchService, documentService, recordImportService, speciesReMatchService
    ActivityFormService activityFormService
    MapService mapService
    PermissionService permissionService
    UserService userService
    EmailService emailService
    HubService hubService
    DataDescriptionService dataDescriptionService
    RecordService recordService
    ProjectActivityService projectActivityService

    @AlaSecured("ROLE_ADMIN")
    def index() {}

    @AlaSecured("ROLE_ADMIN")
    def tools() {}

    @AlaSecured("ROLE_ADMIN")
    def users() {
        def userList = authService.getAllUserNameList()
        [ userNamesList: userList ]
    }

    @RequireApiKey
    def syncCollectoryOrgs() {
        def errors = collectoryService.syncOrganisations(organisationService)
        if (errors)
            render (status: 503, text: errors)
        else
            render (status: 200)
    }

    @AlaSecured("ROLE_ADMIN")
    def settings() {
        def settings = [
                [key:'app.external.model.dir', value: grailsApplication.config.app.external.model.dir,
                        comment: 'location of the application meta-models such as the list of activities and ' +
                                'the output data models'],
                [key:'app.dump.location', value: grailsApplication.config.app.dump.location,
                        comment: 'directory where DB dump files will be created']
        ]
        def config = grailsApplication.config.flatten()
        ['ala.baseURL','grails.serverURL','grails.config.locations','collectory.baseURL',
                'headerAndFooter.baseURL','ecodata.use.uuids'
        ].each {
            settings << [key: it, value: config[it], comment: '']
        }
        [settings: settings]
    }

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json; charset=\"UTF-8\"")
        render model as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def reloadConfig = {
        // clear any cached external config
        cacheService.clear()

        // reload system config
        def resolver = new PathMatchingResourcePatternResolver()
        def resource = resolver.getResource(grailsApplication.config.reloadable.cfgs[0])
        def stream = null

        try {
            stream = resource.getInputStream()
            ConfigSlurper configSlurper = new ConfigSlurper(Environment.current.name)
            if(resource.filename.endsWith('.groovy')) {
                def newConfig = configSlurper.parse(stream.text)
                grailsApplication.getConfig().merge(newConfig)
            }
            else if(resource.filename.endsWith('.properties')) {
                def props = new Properties()
                props.load(stream)
                def newConfig = configSlurper.parse(props)
                grailsApplication.getConfig().merge(newConfig)
            }
            String res = "<ul>"
            grailsApplication.config.each { key, value ->
                if (value instanceof Map) {
                    res += "<p>" + key + "</p>"
                    res += "<ul>"
                    value.each { k1, v1 ->
                        res += "<li>" + k1 + " = " + v1 + "</li>"
                    }
                    res += "</ul>"
                }
                else {
                    res += "<li>${key} = ${value}</li>"
                }
            }
            render res + "</ul>"
        }
        catch (FileNotFoundException fnf) {
            println "No external config to reload configuration. Looking for ${grailsApplication.config.reloadable.cfgs[0]}"
            fnf.printStackTrace()
            render "No external config to reload configuration. Looking for ${grailsApplication.config.reloadable.cfgs[0]}"
        }
        catch (Exception gre) {
            println "Unable to reload configuration. Please correct problem and try again: " + gre.getMessage()
            gre.printStackTrace()
            render "Unable to reload configuration - " + gre.getMessage()
        }
        finally {
            stream?.close()
        }
    }

    @RequireApiKey
    def getBare(String entity, String id) {
        def map = [:]
        switch (entity) {
            case 'activity': map = commonService.toBareMap(Activity.findByActivityId(id)); break
            case 'assessment': map = commonService.toBareMap(Activity.findByActivityId(id)); break
            case 'project': map = commonService.toBareMap(Project.findByProjectId(id)); break
            case 'site': map = commonService.toBareMap(Site.findBySiteId(id)); break
            case 'output': map = commonService.toBareMap(Output.findByOutputId(id)); break
        }
        asJson map
    }

    @AlaSecured("ROLE_ADMIN")
    def showCache() {
        render cacheService.cache
    }

    /**
     * Re-index all docs with ElasticSearch
     */
    @RequireApiKey
    def reIndexAll() {
        def resp = elasticSearchService.indexAll()
        flash.message = "Search index re-indexed - ${resp?.size()} docs"
        render "Indexing done"
    }

    @RequireApiKey
    def clearMetadataCache() {
        // clear any cached external config
        cacheService.clear()
        flash.message = "Metadata cache cleared."
        render 'done'
    }

    @AlaSecured("ROLE_ADMIN")
    def count() {
        def res = [
            projects: Project.collection.count(),
            sites: Site.collection.count(),
            activities: Activity.collection.count(),
            outputs: Output.collection.count()
        ]
        flash.message += res
        render res
    }

    @AlaSecured("ROLE_ADMIN")
    def updateDocumentThumbnails() {

        def results = Document.findAllByStatusAndType('active', 'image')
        results.each { document ->
            documentService.makeThumbnail(document.filepath, document.filename, false)
        }
    }

    /**
     * Refreshes site metadata (geographical facets & geocodes) for every site in the system.
     * @return {"result":"success"} if the operation is successful.
     */
    @AlaSecured("ROLE_ADMIN")
    def reloadSiteMetadata() {
        String dateStr = params.lastUpdatedBefore
        Date date = null
        List fids = params.getList("fids")
        if (dateStr) {
            date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(dateStr)
        }
        siteService.reloadSiteMetadata(fids, date, params.getInt('max', 100))
        Map result = [status:'OK']
        render result as grails.converters.JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def updateSitesWithoutCentroids() {
        def code = 'success'

        def total = 0
        def offset = 0
        def batchSize = 200

        def count = batchSize // For first loop iteration
        while (count == batchSize) {
            def sites = Site.findAllByStatus('active', [offset: offset, max: batchSize]).collect {
                siteService.toMap(it, 'flat')
            }
            count = sites.size()
            try {
                Site.withSession { session -> session.clear() }
                Site.withNewSession {
                    sites.eachWithIndex { site, index ->
                        if (!site.projects) {
                            log.info("Ignoring site ${site.siteId} due to no associated projects")
                            return
                        }
                        def centroid = site.extent?.geometry?.centre
                        if (!centroid) {
                            def updatedSite = siteService.populateLocationMetadataForSite(site)
                            siteService.update([extent: updatedSite.extent], site.siteId, false)
                            total++
                            if(total > 0 && (total % 200) == 0) {
                                log.debug("(${total+1}) records updated in db..")
                            }
                        }
                    }
                }
                log.info("Database updated completed.")
            }
            catch (Exception e) {
                log.error("Unable to complete the operation ", e)
                code = "error"
            }

            offset += batchSize
        }
        def result = [code:code]
        render result as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def linkWithAuth(){
        actor {
            recordImportService.linkWithAuth()
        }
        def model = [started:true]
        render model as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def linkWithImages(){
        actor {
            recordImportService.linkWithImages()
        }
        def model = [started:true]
        render model as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def importFromUrl(){
        def model = [:]

        def url = params.url
        def projectId = params.projectId

        if(projectId && url){

            def project = Project.findByProjectId(projectId)
            if(project){

                def tempOutputFile = "/tmp/import-${System.currentTimeMillis()}.csv"
                if(url){
                    new File(tempOutputFile).withOutputStream { out ->
                        new URL(url).withInputStream { from ->  out << from; }
                    }
                    actor {
                        println "Starting a thread....."
                        recordImportService.loadFile(tempOutputFile, projectId)
                        println "Finishing thread."
                    }
                    model = [started:true]
                } else {
                    model = [started:false, reason:"please supply a file"]
                }

            } else {

                model = [started:false, reason:"Invalid 'projectId' parameter."]
            }

        } else {
            model = [started:false, reason:"Please supply a 'url' and 'projectId' parameter."]
        }
        response.setContentType("application/json")

        render model as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def importFile(){

        def model = [:]

        def filePath = params.filePath
        def projectId = params.projectId

        if(filePath && new File(filePath).exists()){
            actor {
                println "Starting a thread....."
                recordImportService.loadFile(filePath, projectId)
                println "Finishing thread."
            }
            model = [started:true]
        } else {
            model = [started:false, reason:"please supply a file"]
        }

        response.setContentType("application/json")

        render model as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def audit() { }

    @AlaSecured("ROLE_ADMIN")
    def auditMessagesByEntity() { }

    @AlaSecured("ROLE_ADMIN")
    def auditMessagesByProject() { }

    @AlaSecured("ROLE_ADMIN")
    private boolean createStageReportsFromTimeline(project) {
        def timeline = project.timeline

        def dueDateDays = metadataService.programModel(project.associatedProgram).weekDaysToCompleteReport
        def lastActivityEndDate = null
        if (!timeline) {
            log.info "No timeline present for project: ${project.projectId}"
            return false
        }
        if (timeline[0].fromDate >= new DateTime(project.plannedStartDate).plusDays(1).toString()) {
            log.info "WARNING: Project starts before first timeline period for project: ${project.projectId}, timeline start: ${timeline[0].fromDate}, timeline end: ${timeline[0].toDate}, project start: ${new DateTime(project.plannedStartDate).toDateTimeISO()}, programme:${project.associatedProgram}, subprogramme:${project.associatedSubProgram}"
        }
        if (timeline[0].toDate <= new DateTime(project.plannedStartDate).toString() ) {
            log.info "WARNING: Extra timeline period(s) before project start for project: ${project.projectId}, timeline start: ${timeline[0].fromDate}, timeline end: ${timeline[0].toDate}, project start: ${new DateTime(project.plannedStartDate).toDateTimeISO()}, programme:${project.associatedProgram}, subprogramme:${project.associatedSubProgram}"
        }
        if (timeline[timeline.size()-1].fromDate >= new DateTime(project.plannedEndDate).minusDays(1).toString()) {

            def lastActivity = Activity.findAllByProjectId(project.projectId).max{it.plannedEndDate}

            if (lastActivity && lastActivity.plannedEndDate.after(project.plannedEndDate)) {
                log.info("WARNING: activity ends after project end date: ${project.projectId}, project end: ${project.plannedEndDate}, activity end: ${lastActivity.plannedEndDate}")
            }
            log.info "WARNING: Extra timeline period(s) after project end for project: ${project.projectId}, timeline start: ${timeline[timeline.size()-1].fromDate}, timeline end: ${timeline[timeline.size()-1].toDate}, project end: ${new DateTime(project.plannedEndDate).toDateTimeISO()}, programme:${project.associatedProgram}, subprogramme:${project.associatedSubProgram}"
        }
        if (timeline[timeline.size()-1].toDate < new DateTime(project.plannedEndDate).minusDays(1).toString()) {
            log.info "WARNING: Project ends after last timeline period for project: ${project.projectId}, timeline start: ${timeline[timeline.size()-1].fromDate}, timeline end: ${timeline[timeline.size()-1].toDate}, project end: ${new DateTime(project.plannedEndDate).toDateTimeISO()}, programme:${project.associatedProgram}, subprogramme:${project.associatedSubProgram}"
        }

        //def program =//

        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser()

        timeline.each { stage ->

            // Don't create extra reports after the end of the project.
            if (stage.fromDate >= new DateTime(project.plannedEndDate).minusDays(1).toString()  && lastActivityEndDate && stage.fromDate >= new DateTime(lastActivityEndDate).minusDays(1).toString()) {
                return
            }
            if (stage.toDate <= new DateTime(project.plannedStartDate).toString()) {
                // Really need to check what is happening here because we may not be able to not migrate this
                // stage.
                println 'debug me'
            }
            Report report = new Report()
            report.reportId = Identifiers.getNew(true,'')
            report.projectId = project.projectId
            report.name = stage.name
            report.type = 'Activity'
            report.description = stage.name + " report for " + project.name
            report.fromDate = parser.parseDateTime(stage.fromDate).toDate()

            def toDate = parser.parseDateTime(stage.toDate)
            report.toDate = toDate.toDate()

            // Make sure the report can be submitted after the project ends, regardless of when the stage ends.
            if (dueDateDays) {
                if (new DateTime(project.plannedEndDate).toString() < stage.toDate) {
                    report.dueDate = new DateTime(project.plannedEndDate).plusDays(dueDateDays).toDate()
                }
                else {
                    report.dueDate = toDate.plusDays(dueDateDays).toDate()
                }
            }

            report.save(flush:true, failOnError: true)
        }

        return true


    }

    @AlaSecured("ROLE_ADMIN")
    def createStageReports(String projectId) {

        def reports = []
        def project = projectService.get(projectId)
        if (project.isMERIT) {
            reports = Report.findAllByProjectId(project.projectId)
            if (!reports) {
                boolean success = createStageReportsFromTimeline(project)
                if (success) {
                    populateStageReportStatus(project)
                }
            }
        }

        render reports as JSON
    }

    /**
     * a test function to index a project.
     * @return
     */
    def indexProjectDoc() {
        if(params.projectId){
            def projects = Project.findAllByProjectId(params.projectId)

            while (projects) {
                projects.each { project ->
                    try {
                        Map projectMap = elasticSearchService.prepareProjectForHomePageIndex(project)
                        elasticSearchService.indexDoc(projectMap, HOMEPAGE_INDEX)
                    } catch (Exception e) {
                        log.error("Unable to index projewt: " + project?.projectId, e)
                        e.printStackTrace();
                    }
                }
            }
        } else{
            render(status: HttpStatus.SC_BAD_REQUEST, text: 'projectId must be provided')
        }
    }

   /**
    * Initiate species rematch.
    */

    @AlaSecured("ROLE_ADMIN")
    def initiateSpeciesRematch() {
        speciesReMatchService.rematch()
        render ([message:' ok'] as JSON)
    }

    @AlaSecured("ROLE_ADMIN")
    def metadata() {
        [activitiesMetadata: metadataService.activitiesModel()]
    }

    @AlaSecured("ROLE_ADMIN")
    def editActivityFormDefinitions() {
        def model = [availableActivities:activityFormService.activityVersionsByName()]
    }

    @AlaSecured("ROLE_ADMIN")
    def programsModel() {
        List activityTypesList = metadataService.activitiesList().collect {key, value -> [name:key, list:value]}.sort{it.name}

        [programsModel: metadataService.programsModel(), activityTypes:activityTypesList]
    }

    @AlaSecured("ROLE_ADMIN")
    def updateProgramsModel() {
        def model = request.JSON
        log.debug model.toString()
        metadataService.updateProgramsModel(model)
        flash.message = "Programs model updated."
        def result = model
        render result
    }

    @AlaSecured("ROLE_ADMIN")
    def editActivityFormTemplates() {
        def model = [availableActivities:activityFormService.activityVersionsByName()]
        if (params.open) {
            model.open = params.open
        }
        model
    }

    /**
     * Duplicates ActivityFormController.get to implement interactive authorization rules.
     */
    @AlaSecured("ROLE_ADMIN")
    ActivityForm findActivityForm(String name, Integer formVersion) {
        render activityFormService.findActivityForm(name, formVersion) as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def createScore() {
        Score score = new Score([entity:'Activity', configuration:[:]])
        render view:'editScore', model:[score:score]
    }

    @AlaSecured("ROLE_ADMIN")
    def editScore(String id) {
        Score score = Score.findByScoreId(id)

        render view:'editScore', model:[score:score]
    }

    @AlaSecured("ROLE_ADMIN")
    def updateScore(String id) {
        // Using JsonSluper instead of request.JSON to avoid JSONNull being serialized to the String "null" when
        // mapped to a Map type in the domain object.
        def score = request.inputStream.text
        score = new JsonSlurper().parseText(score)

        if (!id) {
            respond metadataService.createScore(score)
        }
        else {
            respond metadataService.updateScore(id, score)
        }
    }

    @AlaSecured("ROLE_ADMIN")
    def deleteScore(String id) {
        respond metadataService.deleteScore(id, params.getBoolean('destroy', false))
    }

    @AlaSecured("ROLE_ADMIN")
    def searchScores() {

        def searchCriteria = request.JSON
        def max = searchCriteria.remove('max') as Integer
        def offset = searchCriteria.remove('offset') as Integer
        String sort = searchCriteria.remove('sort')
        String order = searchCriteria.remove('order')

        BuildableCriteria criteria = Score.createCriteria()
        List scores = criteria.list(max:max ?: 1000, offset:offset) {
            ne("status", DELETED)
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }
            if (sort) {
                order(sort, orderBy?:'asc')
            }

        }
        [scores:scores, count:scores.totalCount]
    }

    @AlaSecured("ROLE_ADMIN")
    /** The synchronization is to prevent a double submit from double creating duplicates */
    synchronized def regenerateRecordsForOutput(String outputId) {
        try {
            Output output = Output.findByOutputId(outputId)
            if (output) {
                Activity activity = Activity.findByActivityId(output.activityId)
                recordService.regenerateRecordsForOutput(output, activity)
                int recordCount = Record.findAllByOutputId(outputId).size()
                flash.message = "Potentially ${recordCount} records affected"
            } else {
                flash.message = "Output not found with id = ${outputId}"
            }
        }
        catch (Exception e) {
            log.error("An error occurred processing output: ${outputId}, message: ${e.message}", e)
            flash.message = "An error occurred processing output: ${outputId}, message: ${e.message}"
        }

        render view:'tools'

    }

    @AlaSecured("ROLE_ADMIN")
    def regenerateRecordsForALAHarvestableProjects() {
        def result = [:]
        try {
            recordService.regenerateRecordsForBioCollectProjects()
            result.message = "Submitted regeneration of records"
        }
        catch (Exception e) {
            log.error("An error occurred regenerating records, message: ${e.message}", e)
            result.message = "An error occurred regenerating records"
        }

        render text: result as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def getIndexNames() {
        Map model = [indexNames: metadataService.getIndicesForDataModels()]
        render view: 'indexNames', model: model
    }

    @AlaSecured("ROLE_ADMIN")
    def updateCollectoryEntryForBiocollectProjects () {
        collectoryService.updateCollectoryEntryForBiocollectProjects()
        render text: [ message: 'Successfully submit synchronisation job.' ] as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def buildGeoServerDependencies() {
        def result = mapService.buildGeoServerDependencies()
        def message, code
        message = result ? "Successfully created GeoServer dependencies" : "Failed to create GeoServer dependencies. Is GeoServer running?"
        code = result ? HttpStatus.SC_OK : HttpStatus.SC_INTERNAL_SERVER_ERROR
        render text: [message: message] as JSON, status: code
    }

    @AlaSecured("ROLE_ADMIN")
    def displayUnIndexedFields() {
        String index = params.get('index', ElasticIndex.HOMEPAGE_INDEX)
        String q = "_ignored:*"
        if (params.q) {
            q += " AND ("+params.q+")"
        }
        List fq = params.getList('fq') ?: []

        List include = params.getList('include') ?: []
        include += ['isMERIT', 'projectId', 'activityId']

        Map params = [fq:fq, include:include, offset:params.getInt('offset', 0), max:params.getInt('max', 100)]
        SearchResponse searchResponse = elasticSearchService.search(q, params, index)

        Map resp = [total:searchResponse.hits.totalHits.value, results:[]]
        searchResponse.hits.hits?.each { SearchHit hit ->
            Map docFields = hit.fields.collectEntries {
                [it.key, it.value.values]
            }
            include.each {
                docFields.put(it, hit.sourceAsMap[it])
            }
            resp.results << [id:hit.docId(), fields: docFields]
        }

        render resp as JSON
    }

    @AlaSecured("ROLE_ADMIN")
    def migrateUserDetailsToEcodata() {
        def resp = permissionService.saveUserDetails()
        render text: [ message: 'UserDetails data migration done.' ] as JSON
    }

    /**
     * Administrative interface to trigger the access expiry job.  Used in MERIT functional
     * tests.
     */
    @AlaSecured("ROLE_ADMIN")
    def triggerAccessExpiryJob() {
        new AccessExpiryJob(
                permissionService: permissionService,
                userService: userService,
                hubService: hubService,
                emailService: emailService).execute()
        render text: [message: 'ok'] as JSON
    }

    /**
     * Administrative interface to trigger the project activity stats update.
     */
    @AlaSecured("ROLE_ADMIN")
    def triggerProjectActivityStatsUpdate() {
        new UpdateProjectActivityStatsJob(
                projectActivityService: projectActivityService,
                cacheService: cacheService,
                grailsApplication: grailsApplication,
                ).execute()
        render 'ok'
    }

    @AlaSecured("ROLE_ADMIN")
    def createDataDescription() {
        if (request.respondsTo('getFile')) {
            MultipartFile excel = request.getFile('descriptionData')
            String fileType = excel.getContentType()

            if (!dataDescriptionService.isValidFileType(fileType) || excel.isEmpty()) {
                flash.errorMessage = 'The uploaded file is empty or is not an excel file'
                redirect(action: 'tools')
                return
            }

            if(dataDescriptionService.importData(excel.getInputStream())){
                flash.message = 'Excel file was uploaded successfully'
                return redirect(action:'tools')
                return
            }
                flash.errorMessage = 'There was error during excel upload'
            return redirect(action:'tools')

        } else {
            response.status = 400
            Map result = [status: 400, error:'No file attachment found']
            response.setContentType('text/plain;charset=UTF8')
            def resultJson = result as JSON
            render resultJson.toString()
        }
    }

}
