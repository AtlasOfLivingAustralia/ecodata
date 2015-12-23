package au.org.ala.ecodata

import grails.converters.JSON
import grails.util.Environment
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

import static groovyx.gpars.actor.Actors.actor

class AdminController {

    private static int DEFAULT_REPORT_DAYS_TO_COMPLETE = 43

    def outputService, activityService, siteService, projectService, authService,
        collectoryService, organisationService,
        commonService, cacheService, metadataService, elasticSearchService, documentService, recordImportService
    def beforeInterceptor = [action:this.&auth, only:['index','tools','settings','audit']]

    /**
     * Triggered by beforeInterceptor, this restricts access to specified (only) actions to ROLE_ADMIN
     * users.
     *
     * @return
     */
    private auth() {
        if (!authService.userInRole(grailsApplication.config.security.cas.adminRole)) {
            flash.message = "You are not authorised to access the page: Administration."
            redirect(uri: "/")
            false
        } else {
            true
        }
    }

    def index() {}
    def tools() {}
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
        model
    }

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
            render "No external config to reload configuration. Looking for ${grailsApplication.config.reloadable.cfgs[0]}"
        }
        catch (Exception gre) {
            println "Unable to reload configuration. Please correct problem and try again: " + gre.getMessage()
            render "Unable to reload configuration - " + gre.getMessage()
        }
        finally {
            stream?.close()
        }
    }

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

    def showCache() {
        render cacheService.cache
    }

    /**
     * Re-index all docs with ElasticSearch
     */
    def reIndexAll() {
        def resp = elasticSearchService.indexAll()
        flash.message = "Search index re-indexed - ${resp?.size()} docs"
        render "Indexing done"
    }

    def clearMetadataCache() {
        // clear any cached external config
        cacheService.clear()
        flash.message = "Metadata cache cleared."
        render 'done'
    }

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
    def reloadSiteMetadata() {

        def code = "success"

        def total = 0
        def offset = 0
        def batchSize = 200

        def count = batchSize // For first loop iteration
        while (count == batchSize) {
            def sites = Site.findAllByStatus('active', [offset:offset, max:batchSize]).collect{siteService.toMap(it, 'flat')}
            count = sites.size()

            try {
                def results = metadataService.getLocationMetadataForSites(sites)

                log.info("Initiating database update..")
                Site.withSession { session -> session.clear() }
                Site.withNewSession {
                    results.eachWithIndex { site, index ->
                        siteService.update([extent: site.extent], site.siteId, false)
                        total++
                        if(total > 0 && (total % 200) == 0) {
                            log.info("(${total+1}) records updated in db..")
                        }
                    }
                }
                log.info("Database updated completed.")
            }
            catch(Exception e) {
                log.error("Unable to complete the operation ", e)
                code = "error"
            }
            offset += batchSize

        }

        def result = [result: "${code}"]
        render result as grails.converters.JSON
    }

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

    def linkWithAuth(){
        actor {
            recordImportService.linkWithAuth()
        }
        def model = [started:true]
        render model as JSON
    }

    def linkWithImages(){
        actor {
            recordImportService.linkWithImages()
        }
        def model = [started:true]
        render model as JSON
    }

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

    def audit() { }
    def auditMessagesByEntity() { }
    def auditMessagesByProject() { }

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

    def populateStageReportStatus(project) {


        List stuff = AuditMessage.findAllByProjectIdAndEventTypeAndEntityType(project.projectId, 'Update', Activity.name)

        stuff.sort { a1, a2 ->
            return a1.date.compareTo(a2.date)
        }

        //println "${project.name}"
        //println "**************"

        def reports = Report.findAllByProjectId(project.projectId)

        stuff.each {
            def status = it.entity.publicationStatus ?: ''
            if (!status) {
                return
            }
            if (it.entity.description == 'Upload of stage 1 and 2 reporting data') {
                return
            }
            def activityEndDate = new DateTime(it.entity.plannedEndDate).minusDays(1).toDate()

            def stageReport = reports.find {
                it.fromDate.before(activityEndDate) &&
                (it.toDate.after(activityEndDate) || it.toDate.equals(activityEndDate))}

            if (!stageReport) {
                throw new Exception("No stage report found for project ${project.projectId} and date ${it.entity.plannedEndDate}")
            }


            if (stageReport.publicationStatus != status) {

                //println "found report: ${stageReport.name} for date ${it.entity.plannedEndDate} status: ${status}"
                switch (status) {
                    case 'pendingApproval':
                        if (stageReport.publicationStatus == 'published') {
                            log.warn("Adding implicit rejection step to the report ${stageReport.name} for project: ${project.projectId}")
                            stageReport.returnForRework("-1", it.date)
                        }
                        stageReport.submit(it.userId, it.date)
                        break
                    case 'published':
                        if (stageReport.publicationStatus != 'pendingApproval') {
                            log.warn("Adding implicit submit step to the report ${stageReport.name} for project: ${project.projectId}")
                            stageReport.submit("-1", it.date)
                        }
                        stageReport.approve(it.userId, it.date)
                        break
                    case 'unpublished':
                        stageReport.returnForRework(it.userId, it.date)
                        break
                }

                stageReport.save()
            }
        }

    }

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


}
