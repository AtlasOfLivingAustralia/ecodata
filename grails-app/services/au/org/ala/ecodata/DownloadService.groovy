package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.CSProjectXlsExporter
import au.org.ala.ecodata.reporting.ProjectExporter
import au.org.ala.ecodata.reporting.ShapefileBuilder
import au.org.ala.ecodata.reporting.StreamingXlsExporter
import au.org.ala.ecodata.reporting.XlsExporter
import grails.async.Promise
import grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.core.TimeValue
import org.elasticsearch.search.SearchHit

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static au.org.ala.ecodata.ElasticIndex.DEFAULT_INDEX
import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX
import static grails.async.Promises.task

class DownloadService {
    ElasticSearchService elasticSearchService
    ProjectService projectService
    ProjectActivityService projectActivityService
    MetadataService metadataService
    DocumentService documentService
    ActivityService activityService
    OutputService outputService
    SiteService siteService
    EmailService emailService
    WebService webService

    def grailsApplication
    def groovyPageRenderer
    def grailsLinkGenerator

    /**
     * Downloads currently selected documents and sends an email to the user when the download is complete.
     * @param params
     */
    void downloadAllDocuments(GrailsParameterMap params) {
        String downloadId = UUID.randomUUID().toString()
        File directoryPath = new File("${grailsApplication.config.getProperty('temp.dir')}")
        directoryPath.mkdirs()
        String fileExtension = params.fileExtension ?: 'zip'
        FileOutputStream outputStream = new FileOutputStream(new File(directoryPath, "${downloadId}.${fileExtension}"))
        // Make the document host url prefix available on the thread running data export
        String documentHostUrlPrefix = DocumentHostInterceptor.documentHostUrlPrefix.get()
        Promise p = task {
            DocumentHostInterceptor.documentHostUrlPrefix.set(documentHostUrlPrefix)
            Project.withNewSession {
                downloadAllDocuments(outputStream, params)
            }
        }

        p.onComplete {
            int days = grailsApplication.config.getProperty('temp.file.cleanup.days', Integer)
            // download url entry point is either BioCollect or MERIT.
            String urlPrefix = params.remove('downloadUrl') ?: grailsApplication.config.getProperty('async.download.url.prefix')
            String url = "${urlPrefix}${downloadId}?fileExtension=${fileExtension}"
            String body = groovyPageRenderer.render(template: "/email/downloadComplete", model:[url: url, days: days])
            emailService.sendEmail("Your download is ready", body, [params.email], [], params.systemEmail, params.senderEmail)
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }

            DocumentHostInterceptor.documentHostUrlPrefix.set(null)
        }

        p.onError { Throwable error ->
            log.error("Failed to generate file for download.", error)
            String body = groovyPageRenderer.render(template: "/email/downloadFailed")
            emailService.sendEmail("Your download has failed", body, [params.email], [], params.systemEmail, params.senderEmail)
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }

            DocumentHostInterceptor.documentHostUrlPrefix.set(null)
        }
    }

    /**
     * Downloads all documents that match the search criteria specified in the params.
     * @param outputStream
     * @param params
     */
    void downloadAllDocuments(FileOutputStream outputStream, GrailsParameterMap params) {
        // this call groups documents by project
        Map<String, Set<String>> documentsByProject = getDocumentIdsForDownload(params)
        long start = System.currentTimeMillis()
        TimeZone timeZone = DateUtil.getTimeZoneFromString(params.clientTimezone) ?: TimeZone.getDefault()
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)
             ByteArrayOutputStream xslFile = new ByteArrayOutputStream()) {
            zipOutputStream.withStream { ZipOutputStream zip ->
                XlsExporter xlsExporter = exportDocumentsToXls(documentsByProject, "data", timeZone)
                zip.putNextEntry(new ZipEntry("data.xlsx"))
                xlsExporter.save(xslFile)
                xslFile.flush()
                zip << xslFile.toByteArray()
                xslFile.flush()
                xslFile.close()
                zip.closeEntry()
            }
        }

        log.info "Exporting documents took ${System.currentTimeMillis() - start} millis"
    }

    /**
     * Group documents by project
     * @param params
     * @param searchIndexName
     * @return
     */
    Map<String, Set<String>> getDocumentIdsForDownload(GrailsParameterMap params, String searchIndexName = DEFAULT_INDEX) {
        long start = System.currentTimeMillis()
        params.put('max', 100)
        // we only need projectId and documentId to group documents by project
        List include = ['projectId', 'documentId']
        params.put('include', include)
        SearchResponse result = elasticSearchService.search(params.query ?:"*:*" , params, searchIndexName, [:], true)
        Map<String, Set<String>> ids = [:].withDefault { new HashSet() }
        while (result.getHits().getHits().length != 0) {
            for (SearchHit hit : result.getHits().getHits()) {
                Map source = hit.sourceAsMap
                if (source.projectId) {
                    ids[source.projectId] << source.documentId
                }
            }

            // get the next batch of results
            result = elasticSearchService.client.scroll(new SearchScrollRequest(result.getScrollId()).scroll(new TimeValue(60000)), RequestOptions.DEFAULT)
        }

        log.info "Query of ${ids.size()} projects took ${System.currentTimeMillis() - start} millis"

        ids
    }

    /**
     * Exports all documents to an sheet on an XLS file.
     * @param documentsByProject
     * @param fileName
     * @param timeZone
     * @return
     */
    XlsExporter exportDocumentsToXls (Map<String, Set<String>> documentsByProject, String fileName, TimeZone timeZone) {
        long start = System.currentTimeMillis()

        XlsExporter xlsExporter = new StreamingXlsExporter(fileName)

        log.info "Exporting documents"

        ProjectExporter projectExporter = new CSProjectXlsExporter(xlsExporter, [:], timeZone)

        log.info "Before exportDocuments projects took ${System.currentTimeMillis() - start} millis"
        start = System.currentTimeMillis()

        projectExporter.exportDocumentsByProjects(documentsByProject)

        log.info "Creating spreadsheet with ${documentsByProject.size()} projects took ${System.currentTimeMillis() - start} millis"

        xlsExporter
    }

    /**
     * Produces the same file as {@link #downloadProjectData(java.io.OutputStream, org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap)}
     * but the file is not streamed back on the HTTP Response: instead, the file is written to disk and an email notification
     * is sent to the requesting user
     *
     * @param params configures the email address, file extension and url path for the download.
     * @param downloadAction must be a Closure taking two parameters, an OutputStream and a Map.
     */
    void downloadProjectDataAsync(GrailsParameterMap params, Closure downloadAction) {
        String downloadId = UUID.randomUUID().toString()
        File directoryPath = new File("${grailsApplication.config.getProperty('temp.dir')}")
        directoryPath.mkdirs()
        String fileExtension = params.fileExtension?:'zip'
        FileOutputStream outputStream = new FileOutputStream(new File(directoryPath, "${downloadId}.${fileExtension}"))
        // Make the document host url prefix available for use by the task as when the reporting server
        // needs document access, it also needs access to this prefix.
        String documentHostUrlPrefix = DocumentHostInterceptor.documentHostUrlPrefix.get()
        Promise p = task {
            DocumentHostInterceptor.documentHostUrlPrefix.set(documentHostUrlPrefix)
            // need to create a new session to ensure that all <entity>.getProperty('dbo') calls work: by default, async
            // calls result in detached entities, which cannot get the underlying Mongo DBObject.
               Project.withNewSession {
                   downloadAction(outputStream, params)
               }
        }
        p.onComplete {
            int days = grailsApplication.config.getProperty('temp.file.cleanup.days', Integer)
            String urlPrefix = params.downloadUrl ?: grailsApplication.config.getProperty('async.download.url.prefix')
            String url = "${urlPrefix}${downloadId}?fileExtension=${fileExtension}"
            String body = groovyPageRenderer.render(template: "/email/downloadComplete", model:[url: url, days: days])
            emailService.sendEmail("Your download is ready", body, [params.email], [], params.systemEmail, params.senderEmail)
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }
        }
        p.onError { Throwable error ->
            log.error("Failed to generate file for download.", error)
            String body = groovyPageRenderer.render(template: "/email/downloadFailed")
            emailService.sendEmail("Your download has failed", body, [params.email], [], params.systemEmail, params.senderEmail)
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }
        }
    }

    void downloadProjectDataAsync(GrailsParameterMap map) {
        Closure doDownload = {OutputStream outputStream, GrailsParameterMap params -> downloadProjectData(outputStream, params)}
        downloadProjectDataAsync(map, doDownload)
    }

    def generateReports(Map params, Closure downloadAction) {
        String downloadId = UUID.randomUUID().toString()
        File directoryPath = new File("${grailsApplication.config.getProperty('temp.dir')}")
        directoryPath.mkdirs()
        String fileExtension = params.fileExtension?:'zip'
        File file = new File(directoryPath, "${downloadId}.${fileExtension}")

        task {
                downloadAction(file)
        }.onComplete {
            int days = grailsApplication.config.getProperty('temp.file.cleanup.days', Integer)
            String url = ''
            // if report url is not supply by FieldCapture, then create a url based on ecodata
            if (!params.reportDownloadBaseUrl)
                url = grailsLinkGenerator.link(controller:'download', action:'get', params:[id: downloadId, fileExtension: fileExtension])
            else
                url = params.reportDownloadBaseUrl+'/' + downloadId+'.'+fileExtension

            String body = groovyPageRenderer.render(template: "/email/downloadComplete", model:[url: url, days: days])
            if(params.email && params.systemEmail && params.senderEmail)
                emailService.sendEmail("Your download is ready", body, [params.email], [], params.systemEmail, params.senderEmail)
            else
                log.error('Email system is missing sender/receiver')

        }.onError { Throwable error ->
            log.error("Failed to generate zip file for download.", error)
            String body = groovyPageRenderer.render(template: "/email/downloadFailed")
            emailService.sendEmail("Your download has failed", body, [params.email], [], params.systemEmail, params.senderEmail)
        }

        return downloadId+'.'+fileExtension
    }

    /**
     * Constructs a zip file with the following structure:
     * |- data.xls --> spreadsheet as per {@link au.org.ala.ecodata.reporting.CSProjectXlsExporter}
     * |- images
     * |--- <projectId> --> one directory for each projectId
     * |--- |- <fileName> --> one file for each project-level image
     * |--- |- activities
     * |--- |- |- <activityId> --> one directory for each activityId
     * |--- |- |- |- <fileName> --> one file for each activity-level image
     * |--- |- |- |- <outputId> --> one directory for each outputId
     * |--- |- |- |--- |- <fileName> --> one file for each output-level image
     * |--- |- |- |- <recordId> --> one directory for each recordId
     * |--- |- |- |--- |- <fileName> --> one file for each record-level image
     * |--- |- records
     * |--- |- |- <recordId> --> one directory for each record occurrenceId
     * |--- |- |- |- <fileName> --> one file for each record-level image
     * |- shapes
     * |--- <projectId> --> one directory for each projectId
     * |--- |- extent.zip --> shapefile for the project extent
     * |--- |- sites.zip  --> shapefile containing all sites for the project
     *
     * @param params
     * @return
     */
    boolean downloadProjectData(OutputStream outputStream, GrailsParameterMap params) {
        elasticSearchService.buildProjectActivityQuery(params)

        Map<String, Set<String>> activitiesByProject = getActivityIdsForDownload(params, PROJECT_ACTIVITY_INDEX)
        Map<String, Object> documentMap = [:] // Accumulates a map of document id to path in zip file

        TimeZone timeZone = DateUtil.getTimeZoneFromString(params.clientTimezone)

        new ZipOutputStream(outputStream).withStream { zip ->
            try{

                addShapeFilesToZip(zip, activitiesByProject.keySet())
                log.debug("Shape files added")

                addImagesToZip(zip, activitiesByProject, documentMap)
                log.debug("Images added")

                XlsExporter xlsExporter = exportProjectsToXls(activitiesByProject, documentMap, "data", timeZone)
                zip.putNextEntry(new ZipEntry("data.xlsx"))
                ByteArrayOutputStream xslFile = new ByteArrayOutputStream()
                xlsExporter.save(xslFile)
                xslFile.flush()
                zip << xslFile.toByteArray()
                xslFile.flush()
                xslFile.close()
                zip.closeEntry()
                log.debug("XLS file added")

                addReadmeToZip(zip, timeZone)
            } catch (Exception e){
                log.error("Error creating download archive", e)
            } finally {
                zip.finish()
                zip.flush()
                zip.close()
            }
        }

        log.debug("ZIP file created")
        true
    }

    private static addReadmeToZip(ZipOutputStream zip, TimeZone timeZone) {
        zip.putNextEntry(new ZipEntry("README.txt"))
        zip << """\
            File format is as follows:

            |- data.xls -> Excel spreadsheet with one tab per survey type, one tab listing all Records, one tab listing all Projects and one tab listing all Sites.
            |- README.txt -> this file
            |- shapefiles
            |- - <project name>
            |- - - projectExtent.zip -> Shape file for the project extent
            |- - - sites.zip -> Shape file containing all Sites associated with the project
            |- images
            |- - <project name>
            |- - - <image files> -> Images associated with the project itself (e.g. logo)
            |- - - activities -> directory structure containing images for the activities and their outputs
            |- - - - <activity name>
            |- - - - - <image files> -> Images associated with the activity itself
            |- - - - - <output name>
            |- - - - - - <image files> -> images associated with an individual Output entity
            |- - - records -> directory structure containing images for individual records not already organised by activty/output
            |- - - - <occurrenceId>
            |- - - - - <image files> -> Images associated with the record
            |- - - records.csv -> Map of record id onto image locations


            This download was produced on ${new Date().format("dd/MM/yyyy HH:mm:ss Z z", timeZone)}.
        """.stripIndent()

        zip.closeEntry()
    }

    private addShapeFilesToZip(ZipOutputStream zip, Set<String> projectIds) {
        long start = System.currentTimeMillis()
        zip.putNextEntry(new ZipEntry("shapefiles/"))

        projectIds.each { projectId ->
            Project project = Project.findByProjectId(projectId)
            def projectName = project.name ?: projectId

            zip.putNextEntry(new ZipEntry("shapefiles/${projectName}/"))
            if (project.projectSiteId) {
                zip.putNextEntry(new ZipEntry("shapefiles/${projectName}/projectExtent.zip"))
                ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
                builder.addSite(project.projectSiteId)
                builder.writeShapefile(zip)
            }

            List<Site> sites = Site.findAllByProjectsAndStatus(projectId, Status.ACTIVE)
            if (sites) {
                zip.putNextEntry(new ZipEntry("shapefiles/${projectName}/sites.zip"))
                ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
                builder.addProject(projectId)
                builder.writeShapefile(zip)
            }
        }
        log.info "Creating shapefiles took ${System.currentTimeMillis() - start} millis"
    }

    private addImagesToZip(ZipOutputStream zip, Map<String, Set<String>> activitiesByProject, Map<String, Object> documentMap) {
        long start = System.currentTimeMillis()
        def paths = [] as Set
        zip.putNextEntry(new ZipEntry("images/"))

        activitiesByProject.each { projectId, activityIds ->
            long currentTimeMillis = System.currentTimeMillis()
            def project = projectService.get(projectId, [ProjectService.BRIEF])
            def projectName = project.name ?: projectId
            def projectPath = makePath("images/${projectName}/", paths)
            def recordMap = [:].withDefault { [] }
            def activityPathBase = makePath("${projectPath}activities/", paths)
            zip.putNextEntry(new ZipEntry(activityPathBase))

          //  log.info "Time taken before groupDocumentsByActivityAndOutput for project ${projectId} is ${System.currentTimeMillis() - currentTimeMillis} millis"
            currentTimeMillis = System.currentTimeMillis()

            def docs = []
            if (activityIds == null || activityIds.isEmpty()) {
                docs = groupProjectDocumentsByActivityAndOutput(projectId)
            } else {
                docs = groupActivityDocumentsByActivityAndOutput (activityIds)
            }

            docs.each { activityId, documentsMap ->
                if (activityId && activityIds?.contains(activityId)) {
                    def activity = activityService.get(activityId, [ActivityService.FLAT])
                    def projectActivity = projectActivityService.get(activity.projectActivityId)
                    def activityName = projectActivity.name ?: activityId
                    def activityPath = makePath("${activityPathBase}${activityName}/", paths)
                    zip.putNextEntry(new ZipEntry(activityPath))

                    documentsMap.each { outputId, documentList ->
                        if (outputId) {
                            def output = outputService.get(outputId)
                            def outputName = output.name ?: outputId
                            def outputPath = makePath("${activityPath}${outputName}/", paths)
                            zip.putNextEntry(new ZipEntry(outputPath))

                            documentList.each { doc ->
                                if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                                    addFileToZip(zip, outputPath, doc, documentMap, paths, true)
                                }
                            }
                        } else {
                            documentList.each { doc ->
                                if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                                    addFileToZip(zip, activityPath, doc, documentMap, paths, true)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                } else {
                    documentsMap[null].each { doc ->
                        if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                            addFileToZip(zip, projectPath, doc, documentMap, paths, true)
                        }
                    }
                }
            }

            zip.closeEntry()

            log.info "Zipping DocumentsByActivityAndOutput images for project ${projectId} took ${System.currentTimeMillis() - currentTimeMillis} millis"

            // put record images into a separate directory structure
            def recordPath = makePath("${projectPath}records/", paths)
            //zip.putNextEntry(new ZipEntry(recordPath))

            currentTimeMillis = System.currentTimeMillis()

            groupDocumentsByRecord(projectId, activityIds).each { recordId, documentList ->
                def recordIdPath = "${recordPath}${recordId}/"
                recordIdPath = makePath(recordIdPath, paths)
                documentList.each { doc ->
                    if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                       // if (!documentMap.containsKey(doc.documentId)) {
                            addFileToZip(zip, recordIdPath, doc, documentMap, paths, true)
                       // }
                        recordMap[recordId] << doc
                    }
                }
            }

            log.info "Total Zipping groupDocumentsByRecord images for project ${projectId} took ${System.currentTimeMillis() - currentTimeMillis} millis"
            if (!recordMap.isEmpty()) {
                zip.putNextEntry(new ZipEntry("${projectPath}records.csv"))
                writeRecordMap(zip, recordMap, documentMap)
                zip.closeEntry()
            }
        }
        log.info "Zipping images took ${System.currentTimeMillis() - start} millis"
    }

    private writeRecordMap(ZipOutputStream zip, Map<String, List<Document>> recordMap, Map<String, Object> documentMap) {
        def encode = { String str ->
            if (!str)
                return ''
            if (str.contains(',') || str.contains('"')) {
                str = str.replaceAll('"', '\\' + '"')
                return '"' + str + '"'
            }
            return str
        }
        zip << "Occurrence Id,Thumbnail,URL,Identifier,Name,Type,Licence,Attribution\n"
        recordMap.keySet().sort().each { recordId ->
            recordMap[recordId].each { doc ->
                zip << encode.call(recordId)
                zip << ","
                zip << encode.call(documentMap[doc.documentId].thumbnail)
                zip << ","
                zip << encode.call(doc.externalUrl)
                zip << ","
                zip << encode.call(doc.identifier)
                zip << ","
                zip << encode.call(doc.name)
                zip << ","
                zip << encode.call(doc.type)
                zip << ","
                zip << encode.call(doc.licence)
                zip << ","
                zip << encode.call(doc.attribution)
                zip << "\n"
            }
        }
    }

    private addFileToZip(ZipOutputStream zip, String zipPath, Document doc, Map<String, Object> documentMap, Set<String> existing, boolean thumbnail = false) {
        String zipName = makePath("${zipPath}${zipPath.endsWith('/') ? '' : '/'}${thumbnail ? Document.THUMBNAIL_PREFIX : ''}${doc.filename}", existing)
        String path = "${grailsApplication.config.getProperty('app.file.upload.path')}${File.separator}${doc.filepath}${File.separator}${doc.filename}"
        File file = new File(path)
        String url

        if (thumbnail) {
            file = documentService.makeThumbnail(doc.filepath, doc.filename, false)
        }
        String thumbnailURL = doc.getThumbnailUrl(true)
        if (file != null && file.exists()) {
            zip.putNextEntry(new ZipEntry(zipName))
            file.withInputStream { i -> zip << i }
        }
        else if (thumbnailURL) {
            // reporting server does not hold images.
            // download it by requesting image from BioCollect/MERIT
            def stream = webService.getStream(thumbnailURL, true)
            if (!(stream instanceof Map)) {
                zip.putNextEntry(new ZipEntry(zipName))
                zip << stream
            }
        } else {
            zipName = zipName + ".notfound"
            zip.putNextEntry(new ZipEntry(zipName))
            log.error("Document exists with file ${doc.filepath}/${doc.filename}, but the corresponding file at ${path} does not exist!")
        }
        documentMap[doc.documentId] = [thumbnail: zipName, externalUrl: doc.externalUrl, identifier: doc.identifier]
        zip.closeEntry()
    }

    private static Map<String, Map<String, List<Document>>> groupProjectDocumentsByActivityAndOutput(String projectId) {
        Map<String, Map<String, List<Document>>> documents = [:].withDefault { [:].withDefault { [] } }

        Activity.findAllByProjectIdAndStatusNotEqual(projectId, Status.DELETED).each { activity ->
            Document.findAllByActivityId(activity.activityId)?.each {
                documents[it.activityId ?: null][it.outputId ?: null] << it
            }
        }

        documents
    }

    private static Map<String, Map<String, List<Document>>> groupActivityDocumentsByActivityAndOutput(Set<String> activityIdsSet) {
        Map<String, Map<String, List<Document>>> documents = [:].withDefault { [:].withDefault { [] } }

        List activityIds = []
        activityIds.addAll(activityIdsSet)

        Document.findAllByActivityIdInListAndStatusNotEqual(activityIds, Status.DELETED)?.each {
            documents[it.activityId ?: null][it.outputId ?: null] << it
        }

        documents
    }

    private static Map<String, List<Document>> groupDocumentsByRecord(String projectId, Set<String> activityIdsSet = null) {
        Map<String, List<Document>> documents = [:].withDefault { [] }

        List documentIds = []
        Map<String, String> recordDocumentMap = [:]

        def recordList
        if (activityIdsSet == null || activityIdsSet.isEmpty()) {
            recordList = Record.findAllByProjectIdAndStatusNotEqual(projectId, Status.DELETED)
        } else {
            List activityIds = []
            activityIds.addAll(activityIdsSet)
            recordList = Record.findAllByProjectIdAndActivityIdInListAndStatusNotEqual(projectId, activityIds, Status.DELETED)
        }

        recordList.each { Record record ->
            record.multimedia?.each { multimedia ->
                if (multimedia.documentId) {
                    recordDocumentMap.put(multimedia.documentId, record.occurrenceID ?: null)
                    documentIds.add(multimedia.documentId)
                }
            }
        }

        List<Document> documentList = Document.findAllByDocumentIdInListAndStatusNotEqual(documentIds, Status.DELETED)

        documentList.each { doc ->
            String recOccurrentId = recordDocumentMap.get(doc.documentId)
            documents[recOccurrentId ?: null] << doc
        }

        documents
    }

    XlsExporter exportProjectsToXls(Map<String, Set<String>> activityIdsByProject, Map<String, Object> documentMap, String fileName = "results", TimeZone timeZone) {
        long start = System.currentTimeMillis()

        XlsExporter xlsExporter = new StreamingXlsExporter(fileName)

        log.info "Exporting activities"

        ProjectExporter projectExporter = new CSProjectXlsExporter(xlsExporter, documentMap, timeZone)

        log.info "Before exportActivities projects took ${System.currentTimeMillis() - start} millis"
        start = System.currentTimeMillis()

        projectExporter.exportActivities(activityIdsByProject)

        log.info "Creating spreadsheet with ${activityIdsByProject.size()} projects took ${System.currentTimeMillis() - start} millis"

        xlsExporter
    }

    Set<String> getProjectIdsForDownload(Map params, String searchIndexName, String property = 'projectId') {
        long start = System.currentTimeMillis()

        params.include = property
        SearchResponse res = elasticSearchService.search(params.query, params, searchIndexName)
        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.sourceAsMap[property]) {
                ids << hit.sourceAsMap[property]
            }
        }

        log.info "Query of ${ids.size()} projects took ${System.currentTimeMillis() - start} millis"

        ids
    }

    Map<String, Set<String>> getActivityIdsForDownload(Map params, String searchIndexName) {
        long start = System.currentTimeMillis()

        Map<String, Set<String>> ids = [:].withDefault { new HashSet() }

        List include = params.getList('include') ?: []
        include += ['projectId', 'activityId']

        params.put('include',include)

        int batchSize = 100
        int processed = 0
        def count = batchSize

        params.max = batchSize

        SearchResponse results = elasticSearchService.search(params.query, params, searchIndexName, [:], true)
        while (results.getHits().getHits().length != 0) {
            for (SearchHit hit : results.getHits().getHits()) {
                Map result = hit.sourceAsMap
                if (result.projectId) {
                    ids[hit.sourceAsMap.projectId] << hit.sourceAsMap.activityId
                }
            }

            results = elasticSearchService.client.scroll(new SearchScrollRequest(results.getScrollId()).scroll(new TimeValue(60000)), RequestOptions.DEFAULT)
        }

        log.info "Query of ${ids.size()} projects took ${System.currentTimeMillis() - start} millis"

        ids
    }

    String makePath(String prototype, Set<String> existing) {
        def dir = prototype.endsWith('/')
        prototype = dir ? prototype.substring(0, prototype.length() - 1) : prototype
        def path = prototype
        int index = 1
        while (existing.contains(path)) {
            path = "${prototype}-${index}"
            index++
        }
        existing << path
        return dir ? path + '/' : path
    }
}
