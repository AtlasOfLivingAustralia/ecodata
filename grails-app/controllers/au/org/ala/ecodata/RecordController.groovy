package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.*
import static org.apache.http.HttpStatus.*

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Base64
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
 * Controller for record CRUD operations with support for handling images.
 */
class RecordController {

    def grailsApplication

    RecordService recordService
    PermissionService permissionService

    static defaultAction = "list"

    def index() {}

    /**
     * Download service for project sightings (for given project only if projectId is given).
     * Optional query param "?model=<modelname>" to restrict output to matching records
     */
    def csvProject() {
        def filename = params.id ? "project-${params.id}.csv" : "projects.csv"
        response.setHeader("Content-Disposition", "attachment; filename=\"${filename}\"");
        response.setContentType("text/csv")
        recordService.exportCSVProject(response.outputStream, params.id, params.model)
    }

    /**
     * Get record by ID (UUID)
     */
    def get() {
        Record record = Record.findByOccurrenceID(params.id)
        if (record) {
            boolean userIsMemberOfProject = params.userId && permissionService.isUserMemberOfProject(params.userId, record.projectId)

            if (record.embargoUntil != null && record.embargoUntil.after(new Date()) && !userIsMemberOfProject) {
                response.sendError(SC_UNAUTHORIZED, "You are not authorised to view this record")
            } else {
                response.setContentType("application/json")
                render recordService.toMap(record) as JSON
            }
        } else {
            response.sendError(SC_NOT_FOUND, 'Unrecognised Record ID. This record may have been removed.')
        }
    }

    /**
     * Retrieve list of records with images.
     */
    def listRecordWithImages() {
        def records = []
        def sort = params.sort ?: "dateCreated"
        def orderBy = params.order ?: "desc"
        def offsetBy = params.start ?: 0
        def max = params.pageSize ?: 10
        String userId = params.userId

        List<String> projectIdsForUser = userId ? permissionService.getProjectsForUser(userId) : []

        def criteria = Record.createCriteria()
        def results = criteria.list {
            isNotNull("multimedia")
            ne("status", DELETED)

            if (userId) {
                'in' "projectId", projectIdsForUser
            } else {
                or {
                    isNull "embargoUntil"
                    lt "embargoUntil", new Date()
                }
            }

            maxResults(max)
            order(sort, orderBy)
            offset(offsetBy)
        }
        results.each {
            records.add(recordService.toMap(it))
        }
        response.setContentType("application/json")
        render records as JSON
    }

    def count() {
        response.setContentType("application/json")
        def model = [count: Record.countByStatusNotEqual(DELETED)]
        render model as JSON
    }

    /**
     * Retrieve a list of records with uncertain identifications.
     */
    def listUncertainIdentifications() {
        log.debug("list request....")

        String userId = params.userId

        List<String> projectIdsForUser = userId ? permissionService.getProjectsForUser(userId) : []

        def ids = Record.withCriteria {
            eq("identificationVerificationStatus", "Uncertain")
            ne("status", DELETED)

            if (userId) {
                'in' "projectId", projectIdsForUser
            } else {
                or {
                    isNull "embargoUntil"
                    lt "embargoUntil", new Date()
                }
            }
        }.collect { it.occurrenceID }
        response.setContentType("application/json")
        def model = ids
        render model as JSON
    }

    /**
     * Retrieve a list of records with paging support.
     */
    def list() {
        log.debug("list request....")

        String sort = params.sort ?: "lastUpdated"
        String orderBy = params.order ?: "desc"
        String offset = params.start ?: 0
        String max = params.pageSize ?: 10
        String userId = params.userId

        List<String> projectIdsForUser = userId ? permissionService.getProjectsForUser(userId) : []

        def query = Record.createCriteria().list(max: max, offset: offset) {
            ne "status", DELETED

            if (userId) {
                'in' "projectId", projectIdsForUser
            } else {
                or {
                    isNull "embargoUntil"
                    lt "embargoUntil", new Date()
                }
            }
            order(sort, orderBy)
        }

        List records = query.collect { recordService.toMap(it) }

        response.setContentType("application/json")
        Map model = [total: query.totalCount, list: records]
        render model as JSON
    }

    /**
     * Retrieve a list of record for the supplied user ID
     */
    def listForUser() {

        log.debug("Retrieving a list for user: ${params.id}")

        def records = []
        def sort = params.sort ?: "lastUpdated"
        def order = params.order ?: "desc"
        def offset = params.offset ?: 0
        def max = params.pageSize ?: 10
        Record.findAllWhere([userId: params.id], [sort: sort, order: order, offset: offset, max: max]).each {
            if (it.status != DELETED) {
                records.add(recordService.toMap(it))
            }
        }
        def totalRecords = Record.countByUserIdAndStatusNotEqual(params.id, DELETED)
        response.setContentType("application/json")
        def model = [total: totalRecords, list: records]
        render model as JSON
    }

    /**
     * Retrieve a list of record for the supplied projectId
     */
    def listForProject() {

        String projectId = params.id
        log.debug("Retrieving a list for project: ${projectId}")

        String sort = params.sort ?: "lastUpdated"
        String orderBy = params.order ?: "desc"
        int startFrom = params.offset ?: 0
        int max = params.pageSize ?: 10
        String userId = params.userId

        boolean projectMember = userId && permissionService.isUserMemberOfProject(userId, projectId)

        def query = Record.createCriteria().list(max: max, offset: startFrom) {
            eq "projectId", projectId
            ne "status", DELETED

            if (!projectMember) {
                or {
                    isNull "embargoUntil"
                    lt "embargoUntil", new Date()
                }
            }

            order(sort, orderBy)
        }

        List records = query.collect { recordService.toMap(it) }

        response.setContentType("application/json")
        Map model = [total: query.totalCount, list: records]
        render model as JSON
    }

    /**
     * Delete by occurrence ID
     */
    @RequireApiKey
    def delete() {
        Record record = Record.findByOccurrenceID(params.id)
        if (record) {
            record.delete(flush: true)
            response.setStatus(SC_OK)
            response.setContentType("application/json")
            def model = [success: true]
            render model as JSON
        } else {
            response.sendError(SC_BAD_REQUEST)
        }
    }

    /**
     * Create method for record. Handles three types of request:
     *
     * 1. Multipart request with 0...n images supplied as files, and a "record" part encoded in JSON.
     * 2. Multipart request with base64 encoded image, and a "record" part encoded in JSON.
     * 3. JSON body post with image supplied via a URL.
     */
    @RequireApiKey
    def create() {

        log.info("Create request received: " + request.getContentType())
        if (request instanceof MultipartHttpServletRequest) {
            try {
                log.info("Multipart POST received ...")
                def js = new JsonSlurper()
                def recordParams = js.parseText(params.record)

                if (!recordParams.userId) {
                    response.setStatus(SC_UNAUTHORIZED)
                    response.setContentType("application/json")
                    return [success: false, message: "userId not specified"]
                }

                if (recordParams) {

                    def imagesToBeLoaded = [:]

                    //handle the multipart message.....
                    if (request instanceof MultipartHttpServletRequest) {
                        Map<String, MultipartFile> fileMap = request.getFileMap()
                        for (String fileKey : fileMap.keySet()) {
                            MultipartFile multipartFile = fileMap.get(fileKey)
                            byte[] imageAsBytes = multipartFile.getBytes()
                            String originalName = multipartFile.getOriginalFilename()
                            imagesToBeLoaded.put(originalName, imageAsBytes)
                        }
                    }

                    //handle image base64 encoded
                    if (params.imageBase64 && params.imageFileName) {
                        byte[] imageAsBytes = Base64.decodeBase64(params.imageBase64)
                        imagesToBeLoaded.put(params.imageFileName, imageAsBytes)
                    }

                    //save the record
                    def (record, errors) = recordService.createRecordWithImages(recordParams, imagesToBeLoaded)
                    if (errors.size() == 0) {
                        log.debug "Added record: ${record.occurrenceID}"
                        response.setStatus(SC_OK)
                        setResponseHeadersForRecord(response, record)
                        response.setContentType("application/json;charset=UTF-8")
                        [message: 'created', recordId: record.occurrenceID, occurrenceID: record.occurrenceID]
                    } else {
                        log.error "Problem creating record. ${errors}"
                        record.delete(flush: true)
                        response.addHeader 'errors', (errors as grails.converters.JSON).toString()
                        response.sendError(SC_BAD_REQUEST, "Unable to create a new record. See errors for more details.")
                    }
                } else {
                    response.setContentType("application/json")
                    log.error("Unable to create record. " + errors)
                    response.sendError(SC_BAD_REQUEST, errors)
                    [success: false]
                }

            } catch (Exception e) {
                response.setStatus(SC_INTERNAL_SERVER_ERROR)
                response.setContentType("application/json")
                [success: false]
            }
        } else {
            log.info("JSON POST received ...")
            //if not multi part request, expect a JSON body
            def json = request.JSON
            if (json.userId) {
                def (record, errors) = recordService.createRecord(json)
                if (errors.size() == 0) {
                    setResponseHeadersForRecord(response, record)
                    response.setContentType("application/json;charset=UTF-8")
                    [message: 'created', recordId: record.occurrenceID, occurrenceID: record.occurrenceID]
                } else {
                    record.delete(flush: true)
                    response.addHeader 'errors', (errors as grails.converters.JSON).toString()
                    response.sendError(SC_BAD_REQUEST, "Unable to create a new record. See errors for more details.")
                }
            } else {
                response.sendError(SC_BAD_REQUEST, 'Missing userId')
            }
        }
    }

    /**
     * Update the supplied record.
     */
    @RequireApiKey
    def update() {
        def json = request.JSON
        Record record = Record.findByOccurrenceID(params.id)
        Map errors = recordService.updateRecord(record, json)

        log.debug "Errors = ${errors}"

        setResponseHeadersForRecord(response, record)

        //add the errors to the header too
        response.addHeader('errors', (errors as grails.converters.JSON).toString())
        if (errors) {
            response.sendError(SC_BAD_REQUEST, (errors as JSON).toString())
        } else {
            render record as JSON
        }
    }

    private def setResponseHeadersForRecord(response, record) {
        response.addHeader("content-location", grailsApplication.config.grails.serverURL + "/record/" + record.occurrenceID)
        response.addHeader("location", grailsApplication.config.grails.serverURL + "/record/" + record.occurrenceID)
        response.addHeader("entityId", record.id.toString())
    }
}