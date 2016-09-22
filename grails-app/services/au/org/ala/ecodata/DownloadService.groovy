package au.org.ala.ecodata

import au.org.ala.ecodata.reporting.CSProjectXlsExporter
import au.org.ala.ecodata.reporting.ProjectExporter
import au.org.ala.ecodata.reporting.ShapefileBuilder
import au.org.ala.ecodata.reporting.XlsExporter
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX
import static grails.async.Promises.task

class DownloadService {
    ElasticSearchService elasticSearchService
    ProjectService projectService
    MetadataService metadataService
    DocumentService documentService
    ActivityService activityService
    SiteService siteService
    EmailService emailService

    def grailsApplication
    def groovyPageRenderer

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
        File directoryPath = new File("${grailsApplication.config.temp.dir}")
        directoryPath.mkdirs()
        String fileExtension = params.fileExtension?:'zip'
        FileOutputStream outputStream = new FileOutputStream(new File(directoryPath, "${downloadId}.${fileExtension}"))

        task {
            // need to create a new session to ensure that all <entity>.getProperty('dbo') calls work: by default, async
            // calls result in detached entities, which cannot get the underlying Mongo DBObject.
            Project.withNewSession {
                downloadAction(outputStream, params)
            }
        }.onComplete {
            int days = grailsApplication.config.temp.file.cleanup.days as int
            String urlPrefix = params.downloadUrl ?: grailsApplication.config.async.download.url.prefix
            String url = "${urlPrefix}${downloadId}?fileExtension=${fileExtension}"
            String body = groovyPageRenderer.render(template: "/email/downloadComplete", model:[url: url, days: days])
            emailService.sendEmail("Your download is ready", body, [params.email], [], params.systemEmail, params.senderEmail)
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }
        }.onError { Throwable error ->
            log.error("Failed to generate zip file for download.", error)
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

        XlsExporter xlsExporter = exportProjectsToXls(activitiesByProject, "data")

        new ZipOutputStream(outputStream).withStream { zip ->
            try{
                zip.putNextEntry(new ZipEntry("data.xls"))
                ByteArrayOutputStream xslFile = new ByteArrayOutputStream()
                xlsExporter.save(xslFile)
                xslFile.flush()
                zip << xslFile.toByteArray()
                xslFile.flush()
                xslFile.close()
                zip.closeEntry()
                log.debug("XLS file added")

                addShapeFilesToZip(zip, activitiesByProject.keySet())
                log.debug("Shape files added")

                addImagesToZip(zip, activitiesByProject)
                log.debug("Images added")

                addReadmeToZip(zip)
            } catch (Exception e){
                log.error(e.message)
                log.error(e.stackTrace)
            } finally {
                zip.finish()
                zip.flush()
                zip.close()
            }
        }

        log.debug("ZIP file created")
        true
    }

    private static addReadmeToZip(ZipOutputStream zip) {
        zip.putNextEntry(new ZipEntry("README.txt"))
        zip << """\
            File format is as follows:

            |- data.xls -> Excel spreadsheet with one tab per survey type, one tab listing all Records, one tab listing all Projects and one tab listing all Sites.
            |- README.txt -> this file
            |- shapefiles
            |- - <projectId>
            |- - - projectExtent.zip -> Shape file for the project extent
            |- - - sites.zip -> Shape file containing all Sites associated with the project
            |- images
            |- - <projectId>
            |- - - <image files> -> Images associated with the project itself (e.g. logo)
            |- - - activities -> directory structure containing images for the activities and their outputs
            |- - - - <activityId>
            |- - - - - <image files> -> Images associated with the activity itself
            |- - - - - <outputId>
            |- - - - - - <image files> -> images associated with an individual Output entity
            |- - - records -> directory structure containing images for individual records
            |- - - - <occurrenceId>
            |- - - - - <image files> -> Images associated with the record


            This download was produced on ${new Date().format("dd/MM/yyyy HH:mm")}.
        """.stripIndent()

        zip.closeEntry()
    }

    private addShapeFilesToZip(ZipOutputStream zip, Set<String> projectIds) {
        long start = System.currentTimeMillis()
        zip.putNextEntry(new ZipEntry("shapefiles/"))

        projectIds.each { projectId ->
            zip.putNextEntry(new ZipEntry("shapefiles/${projectId}/"))

            Project project = Project.findByProjectId(projectId)
            if (project.projectSiteId) {
                zip.putNextEntry(new ZipEntry("shapefiles/${projectId}/projectExtent.zip"))
                ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
                builder.addSite(project.projectSiteId)
                builder.writeShapefile(zip)
            }

            List<Site> sites = Site.findAllByProjectsAndStatus(projectId, Status.ACTIVE)
            if (sites) {
                zip.putNextEntry(new ZipEntry("shapefiles/${projectId}/sites.zip"))
                ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
                builder.addProject(projectId)
                builder.writeShapefile(zip)
            }
        }
        log.info "Creating shapefiles took ${System.currentTimeMillis() - start} millis"
    }

    private addImagesToZip(ZipOutputStream zip, Map<String, Set<String>> activitiesByProject) {
        long start = System.currentTimeMillis()
        zip.putNextEntry(new ZipEntry("images/"))

        activitiesByProject.each { projectId, activityIds ->
            zip.putNextEntry(new ZipEntry("images/${projectId}/activities/"))

            groupDocumentsByActivityAndOutput(projectId).each { activityId, documentsMap ->
                if (activityId && activityIds?.contains(activityId)) {
                    zip.putNextEntry(new ZipEntry("images/${projectId}/activities/${activityId}/"))

                    documentsMap.each { outputId, documentList ->
                        if (outputId) {
                            zip.putNextEntry(new ZipEntry("images/${projectId}/activities/${activityId}/${outputId}/"))

                            documentList.each { doc ->
                                if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                                    addFileToZip(zip, "images/${projectId}/activities/${activityId}/${outputId}/", doc)
                                }
                            }
                        } else {
                            documentList.each { doc ->
                                if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                                    addFileToZip(zip, "images/${projectId}/activities/${activityId}/", doc)
                                }
                            }
                        }
                    }

                    zip.closeEntry()
                } else {
                    documentsMap[null].each { doc ->
                        if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                            addFileToZip(zip, "images/${projectId}/", doc)
                        }
                    }
                }
            }

            zip.closeEntry()

            // put record images into a separate directory structure
            zip.putNextEntry(new ZipEntry("images/${projectId}/records/"))

            groupDocumentsByRecord(projectId).each { recordId, documentList ->
                if (documentList) {
                    zip.putNextEntry(new ZipEntry("images/${projectId}/records/${recordId}/"))

                    documentList.each { doc ->
                        if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                            addFileToZip(zip, "images/${projectId}/records/${recordId}/", doc)
                        }
                    }

                    zip.closeEntry()
                }
            }

            zip.closeEntry()
        }


        log.info "Zipping images took ${System.currentTimeMillis() - start} millis"
    }

    private addFileToZip(ZipOutputStream zip, String zipPath, Document doc) {
        zip.putNextEntry(new ZipEntry("${zipPath}/${doc.filename}"))

        String path = "${grailsApplication.config.app.file.upload.path}${File.separator}${doc.filepath}${File.separator}${doc.filename}"

        File file = new File(path)

        if (file.exists()) {
            file.withInputStream { i -> zip << i }
        } else {
            log.error("Document exists with file ${doc.filepath}/${doc.filename}, but the corresponding file at ${path} does not exist!")
        }

        zip.closeEntry()
    }

    private static Map<String, Map<String, List<Document>>> groupDocumentsByActivityAndOutput(String projectId) {
        Map<String, Map<String, List<Document>>> documents = [:].withDefault { [:].withDefault { [] } }

        Activity.findAllByProjectIdAndStatusNotEqual(projectId, Status.DELETED).each { activity ->
            Document.findAllByActivityId(activity.activityId)?.each {
                documents[it.activityId ?: null][it.outputId ?: null] << it
            }
        }

        documents
    }

    private static Map<String, List<Document>> groupDocumentsByRecord(String projectId) {
        Map<String, List<Document>> documents = [:].withDefault { [] }

        Record.findAllByProjectIdAndStatusNotEqual(projectId, Status.DELETED).each { Record record ->
            record.multimedia?.each { multimedia ->
                Document.findAllByDocumentId(multimedia.documentId)?.each { doc ->
                    documents[record.occurrenceID ?: null] << doc
                }
            }
        }

        documents
    }

    XlsExporter exportProjectsToXls(Map<String, Set<String>> activityIdsByProject, String fileName = "results") {
        long start = System.currentTimeMillis()

        XlsExporter xlsExporter = new XlsExporter(fileName)

        ProjectExporter projectExporter = new CSProjectXlsExporter(xlsExporter)

        projectExporter.exportActivities(activityIdsByProject)

        log.info "Creating spreadsheet with ${activityIdsByProject.size()} projects took ${System.currentTimeMillis() - start} millis"

        xlsExporter
    }

    Set<String> getProjectIdsForDownload(Map params, String searchIndexName, String property = 'projectId') {
        long start = System.currentTimeMillis()

        SearchResponse res = elasticSearchService.search(params.query, params, searchIndexName)
        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.source[property]) {
                ids << hit.source[property]
            }
        }

        log.info "Query of ${ids.size()} projects took ${System.currentTimeMillis() - start} millis"

        ids
    }

    Map<String, Set<String>> getActivityIdsForDownload(Map params, String searchIndexName) {
        long start = System.currentTimeMillis()

        SearchResponse res = elasticSearchService.search(params.query, params, searchIndexName)
        Map<String, Set<String>> ids = [:].withDefault { new HashSet() }

        for (SearchHit hit : res.hits.hits) {
            if (hit.source.projectId) {
                ids[hit.source.projectId] << hit.source.activityId
            }
        }

        log.info "Query of ${ids.size()} projects took ${System.currentTimeMillis() - start} millis"

        ids
    }
}
