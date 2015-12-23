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

    /**
     * Produces the same file as {@link #downloadProjectData(java.io.OutputStream, org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap)}
     * but the file is not streamed back on the HTTP Response: instead, the file is written to disk and an email notification
     * is sent to the requesting user
     *
     * @param params
     */
    void downloadProjectDataAsync(GrailsParameterMap params) {
        String downloadId = UUID.randomUUID().toString()
        File directoryPath = new File("${grailsApplication.config.temp.dir}")
        directoryPath.mkdirs()
        FileOutputStream outputStream = new FileOutputStream(new File(directoryPath, "${downloadId}.zip"))

        task {
            // need to create a new session to ensure that all <entity>.getProperty('dbo') calls work: by default, async
            // calls result in detached entities, which cannot get the underlying Mongo DBObject.
            Project.withNewSession {
                downloadProjectData(outputStream, params)
            }
        }.onComplete {
            int days = grailsApplication.config.temp.file.cleanup.days as int
            String url = "${grailsApplication.config.async.download.url.prefix}${downloadId}"
            String body = """
                        <html><body>
                        <p>You may download your file from <a href="${url}">this link</a>.</p>
                        <p>Download files are automatically deleted from the server after ${days > 1 ? days + ' days' : ' 24 hours'}.
                            If you have not downloaded your file before then, you will need to request a new download.</p>
                        <p>This is an automated email. Please do not reply.</p>
                        </body>
                        </html>
                        """
            emailService.sendEmail("Your download is ready", body, [params.email])
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }
        }.onError { Throwable error ->
            log.error("Failed to generate zip file for download.", error)
            emailService.sendEmail("Your download has failed", "An unexpected error has occurred while creating your download. Please try again.", [params.email])
            if (outputStream) {
                outputStream.flush()
                outputStream.close()
            }
        }
    }

    /**
     * Constructs a zip file with the following structure:
     * |- data.xls --> spreadsheet as per {@link au.org.ala.ecodata.reporting.CSProjectXlsExporter}
     * |- images
     * |--- <projectId> --> one directory for each projectId
     * |--- |- <fileName> --> one file for each project-level image
     * |--- |- <activityId> --> one directory for each activityId
     * |--- |- |- <fileName> --> one file for each activity-level image
     * |--- |- |- <outputId> --> one directory for each outputId
     * |--- |- |--- |- <fileName> --> one file for each output-level image
     * |--- |- |--- |- |- <recordId> --> one directory for each recordId
     * |--- |- |--- |- |--- <fileName> --> one file for each record-level image
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

            zip.finish()
            zip.flush()
            zip.close()
        }

        log.debug("ZIP file created")
        true
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
            zip.putNextEntry(new ZipEntry("images/${projectId}/"))

            groupDocumentsByActivityAndOutput(projectId).each { activityId, documentsMap ->
                if (activityId && activityIds?.contains(activityId)) {
                    zip.putNextEntry(new ZipEntry("images/${projectId}/${activityId}/"))

                    documentsMap.each { outputId, documentList ->
                        if (outputId) {
                            zip.putNextEntry(new ZipEntry("images/${projectId}/${activityId}/${outputId}/"))

                            documentList.each { doc ->
                                if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                                    addFileToZip(zip, "images/${projectId}/${activityId}/${outputId}/", doc)
                                }
                            }
                        } else {
                            documentList.each { doc ->
                                if (doc.type == Document.DOCUMENT_TYPE_IMAGE) {
                                    addFileToZip(zip, "images/${projectId}/${activityId}/", doc)
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

    XlsExporter exportProjectsToXls(Map<String, Set<String>> activityIdsByProject, String fileName = "results") {
        long start = System.currentTimeMillis()

        XlsExporter xlsExporter = new XlsExporter(fileName)

        ProjectExporter projectExporter = new CSProjectXlsExporter(xlsExporter)

        projectExporter.exportActivities(activityIdsByProject)

        log.info "Creating spreadsheet with ${activityIdsByProject.size()} projects took ${System.currentTimeMillis() - start} millis"

        xlsExporter
    }

    Set<String> getProjectIdsForDownload(Map params, String searchIndexName) {
        long start = System.currentTimeMillis()

        SearchResponse res = elasticSearchService.search(params.query, params, searchIndexName)
        Set ids = new HashSet()

        for (SearchHit hit : res.hits.hits) {
            if (hit.source.projectId) {
                ids << hit.source.projectId
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
