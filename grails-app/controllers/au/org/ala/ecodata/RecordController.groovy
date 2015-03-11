package au.org.ala.ecodata
import au.com.bytecode.opencsv.CSVWriter
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.http.impl.cookie.DateUtils
import org.bson.types.ObjectId

/**
 * Controller for record CRUD operations.
 */
class RecordController {

    def grailsApplication

    def recordService

    static defaultAction = "list"

    /**
     * Download service for all records.
     */
    def csv(){
        response.setHeader("Content-Disposition","attachment; filename=\"records.csv\"");
        response.setContentType("text/csv")
        def cutoff = null
        if(params.lastUpdated){
            if(params.lastUpdated.toLowerCase() == "day") {
                cutoff = new Date().minus(1)
            } else if(params.lastUpdated.toLowerCase() == "week"){
                cutoff = new Date().minus(7)
            } else if(params.lastUpdated.toLowerCase() == "month"){
                cutoff = new Date().minus(30)
            } else if(params.lastUpdated.toLowerCase() == "year"){
                cutoff = new Date().minus(365)
            } else {
                cutoff = DateUtils.parseDate(params.lastUpdated, ["yyyy-MM-dd"] as String[])
            }
        }
        recordService.exportCSV(response.outputStream, cutoff)
    }

    /**
     * Get record by ID (UUID)
     */
    def getById(){
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
            isNotNull("associatedMedia")
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
     * Retrieve a list of records with paging support.
     */
    def list(){
        log.debug("list request....")
        def records = []
        def sort = params.sort ?: "dateCreated"
        def order = params.order ?:  "desc"
        def offset = params.start ?: 0
        def max = params.pageSize ?: 10
        Record.list([sort:sort,order:order,offset:offset,max:max]).each {
            recordService.toMap(it)
            records.add(recordService.toMap(it))
        }
        def totalRecords = Record.count()
        response.setContentType("application/json")
        def model = [totalRecords: totalRecords, records:records]
        render model as JSON
    }

    /**
     * Retrieve a list of record for the supplied user ID
     */
    def listForUser(){
        log.debug("list request for user...." + params.userId)
        def records = []
        def sort = params.sort ?: "dateCreated"
        def order = params.order ?:  "desc"
        def offset = params.start ?: 0
        def max = params.pageSize ?: 10

        log.debug("Retrieving a list for user:"  + params.userId)
        Record.findAllWhere([userId:params.userId], [sort:sort,order:order,offset:offset,max:max]).each {
            records.add(recordService.toMap(it))
        }
        def totalRecords = Record.countByUserId(params.userId)
        response.setContentType("application/json")
        def model = [totalRecords: totalRecords, records:records]
        render model as JSON
    }

    /**
     * Delete by occurrence ID
     */
    def deleteById(){
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
     * Create method with JSON body...
     */
    def create(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        if (json.userId){
            def (record, errors) = recordService.createRecord(json)
            if(errors.size() == 0){
                setResponseHeadersForRecord(response, record)
                response.setContentType("application/json")
                def model = recordService.toMap(record)
                render model as JSON
            } else {
                record.delete(flush: true)
                response.addHeader 'errors', (errors as grails.converters.JSON).toString()
                response.sendError(400, "Unable to create a new record. See errors for more details." )
            }
        } else {
            response.sendError(400, 'Missing userId')
        }
    }

    /**
     * Update the supplied record.
     */
    def updateById(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        //json.eventDate = new Date().parse("yyyy-MM-dd", json.eventDate)
        //TODO add some data validation....
        Record record = Record.findByOccurrenceID(params.id)
        Map errors = recordService.updateRecord(record,json)
        log.debug "updateById() - errors = ${errors}"
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