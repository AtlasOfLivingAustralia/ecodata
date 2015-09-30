package au.org.ala.ecodata
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

    def recordService

    static defaultAction = "list"

    def index(){}

    /**
     * Download service for project sightings (for given project only if projectId is given).
     * Optional query param "?model=<modelname>" to restrict output to matching records
     */
    def csvProject(){
        def filename = params.id? "project-${params.id}.csv": "projects.csv"
        response.setHeader("Content-Disposition","attachment; filename=\"${filename}\"");
        response.setContentType("text/csv")
        recordService.exportCSVProject(response.outputStream, params.id, params.model)
    }

    /**
     * Get record by ID (UUID)
     */
    def get(){
        Record record = Record.findByOccurrenceID(params.id)
        if(record){
            response.setContentType("application/json")
            def model = recordService.toMap(record)
            render model as JSON
        } else {
            response.sendError(404, 'Unrecognised Record ID. This record may have been removed.')
        }
    }

    /**
     * Retrieve list of records with images.
     */
    def listRecordWithImages(){
        def records = []
        def sort = params.sort ?: "dateCreated"
        def orderBy = params.order ?:  "desc"
        def offsetBy = params.start ?: 0
        def max = params.pageSize ?: 10

        def criteria = Record.createCriteria()
        def results = criteria.list {
            isNotNull("multimedia")
            maxResults(max)
            order(sort,orderBy)
            offset(offsetBy)
        }
        results.each {
            records.add(recordService.toMap(it))
        }
        response.setContentType("application/json")
        render records as JSON
    }

    /**
     * Retrieve the current record count.
     */
    def count(){
        response.setContentType("application/json")
        def model = [count:Record.count()]
        render model as JSON
    }

    /**
     * Retrieve a list of records with uncertain identifications.
     */
    def listUncertainIdentifications(){
        log.debug("list request....")
        def ids = Record.findAllWhere( ["identificationVerificationStatus" : "Uncertain"]).collect { it.occurrenceID }
        response.setContentType("application/json")
        def model = ids
        render model as JSON
    }

    /**
     * Retrieve a list of records with paging support.
     */
    def list(){
        log.debug("list request....")
        def records = []
        def sort = params.sort ?: "lastUpdated"
        def order = params.order ?:  "desc"
        def offset = params.start ?: 0
        def max = params.pageSize ?: 10
        Record.list([sort:sort,order:order,offset:offset,max:max]).each {
            recordService.toMap(it)
            records.add(recordService.toMap(it))
        }
        def totalRecords = Record.count()
        response.setContentType("application/json")
        def model = [total: totalRecords, list:records]
        render model as JSON
    }

    /**
     * Retrieve a list of record for the supplied user ID
     */
    def listForUser(){

        log.debug("Retrieving a list for user: ${params.id}")

        def records = []
        def sort = params.sort ?: "lastUpdated"
        def order = params.order ?:  "desc"
        def offset = params.offset ?: 0
        def max = params.pageSize ?: 10
        Record.findAllWhere([userId:params.id], [sort:sort,order:order,offset:offset,max:max]).each {
            records.add(recordService.toMap(it))
        }
        def totalRecords = Record.countByUserId(params.id)
        response.setContentType("application/json")
        def model = [total: totalRecords, list:records]
        render model as JSON
    }

    /**
     * Delete by occurrence ID
     */
    @RequireApiKey
    def delete(){
        Record record = Record.findByOccurrenceID(params.id)
        if (record){
            record.delete(flush: true)
            response.setStatus(200)
            response.setContentType("application/json")
            def model = [success:true]
            render model as JSON
        } else {
            response.sendError(400)
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
    def create(){

        log.info("Create request received: " + request.getContentType())
        if(request instanceof MultipartHttpServletRequest){
            try {
                log.info("Multipart POST received ...")
                def js = new JsonSlurper()
                def recordParams = js.parseText(params.record)

                if (!recordParams.userId) {
                    response.setStatus(403)
                    response.setContentType("application/json")
                    return [success:false, message:"userId not specified"]
                }

                if (recordParams){

                    def imagesToBeLoaded = [:]

                    //handle the multipart message.....
                    if(request instanceof MultipartHttpServletRequest){
                        Map<String, MultipartFile> fileMap = request.getFileMap()
                        for(String fileKey: fileMap.keySet()){
                            MultipartFile multipartFile = fileMap.get(fileKey)
                            byte[] imageAsBytes = multipartFile.getBytes()
                            String originalName = multipartFile.getOriginalFilename()
                            imagesToBeLoaded.put(originalName, imageAsBytes)
                        }
                    }

                    //handle image base64 encoded
                    if(params.imageBase64 && params.imageFileName){
                        byte[] imageAsBytes = Base64.decodeBase64(params.imageBase64)
                        imagesToBeLoaded.put(params.imageFileName, imageAsBytes)
                    }

                    //save the record
                    def (record, errors) = recordService.createRecordWithImages(recordParams, imagesToBeLoaded)
                    if(errors.size() == 0){
                        log.debug "Added record: ${record.occurrenceID}"
                        response.setStatus(200)
                        setResponseHeadersForRecord(response, record)
                        response.setContentType("application/json;charset=UTF-8")
                        [message: 'created', recordId: record.occurrenceID, occurrenceID: record.occurrenceID]
                    } else {
                        log.error "Problem creating record. ${errors}"
                        record.delete(flush: true)
                        response.addHeader 'errors', (errors as grails.converters.JSON).toString()
                        response.sendError(400, "Unable to create a new record. See errors for more details." )
                    }
                } else {
                    response.setContentType("application/json")
                    log.error("Unable to create record. " + errors)
                    response.sendError(400, errors)
                    [success:false]
                }

            } catch (Exception e){
                response.setStatus(500)
                response.setContentType("application/json")
                [success:false]
            }
        } else {
            log.info("JSON POST received ...")
            //if not multi part request, expect a JSON body
            def json = request.JSON
            if (json.userId){
                def (record, errors) = recordService.createRecord(json)
                if(errors.size() == 0){
                    setResponseHeadersForRecord(response, record)
                    response.setContentType("application/json;charset=UTF-8")
                    [message: 'created', recordId: record.occurrenceID, occurrenceID: record.occurrenceID]
                } else {
                    record.delete(flush: true)
                    response.addHeader 'errors', (errors as grails.converters.JSON).toString()
                    response.sendError(400, "Unable to create a new record. See errors for more details." )
                }
            } else {
                response.sendError(400, 'Missing userId')
            }
        }
    }

    /**
     * Update the supplied record.
     */
    @RequireApiKey
    def update(){
        def json = request.JSON
        Record record = Record.findByOccurrenceID(params.id)
        Map errors = recordService.updateRecord(record, json)

        log.debug "Errors = ${errors}"

        setResponseHeadersForRecord(response, record)

        //add the errors to the header too
        response.addHeader('errors', (errors as grails.converters.JSON).toString())
        if (errors) {
            response.sendError(400, (errors as JSON).toString())
        } else {
            render record as JSON
        }
    }

    private def setResponseHeadersForRecord(response, record){
        response.addHeader("content-location", grailsApplication.config.grails.serverURL + "/record/" + record.occurrenceID)
        response.addHeader("location", grailsApplication.config.grails.serverURL + "/record/" + record.occurrenceID)
        response.addHeader("entityId", record.id.toString())
    }
}