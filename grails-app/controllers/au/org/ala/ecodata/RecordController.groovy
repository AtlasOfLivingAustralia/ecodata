package au.org.ala.ecodata
import au.com.bytecode.opencsv.CSVWriter
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.bson.types.ObjectId

class RecordController {

    def grailsApplication

    def mediaService

    def broadcastService

    def recordService

    static defaultAction = "list"

    def csv(){

        response.setHeader("Content-Disposition","attachment; filename=\"records.csv\"");
        response.setContentType("text/csv")
        def csvWriter = new CSVWriter(new OutputStreamWriter(response.outputStream))
        csvWriter.writeNext(
            [
              "occurrenceID",
              "scientificName",
              "family",
              "kingdom",
              "decimalLatitude",
              "decimalLongitude",
              "eventDate",
              "userId",
              "recordedBy",
              "usingReverseGeocodedLocality",
              "individualCount",
              "submissionMethod",
              "georeferenceProtocol",
              "identificationVerificationStatus",
              "occurrenceRemarks",
              "coordinateUncertaintyInMeters",
              "geodeticDatum",
              "imageLicence",
              "locality",
              "associatedMedia",
              "modified",
            ] as String[]
        )

        Record.list().each {

          def map = recordService.toMap(it)

          csvWriter.writeNext(
            [
             map.occurrenceID?:"",
             map.scientificName?:"",
             map.family?:"",
             map.kingdom?:"",
             map.decimalLatitude?:"",
             map.decimalLongitude?:"",
             map.eventDate?:"",
             map.userId?:"",
             map.recordedBy?:"",
             map.usingReverseGeocodedLocality?:"",
             map.individualCount?:"",
             map.submissionMethod?:"",
             map.georeferenceProtocol?:"",
             map.identificationVerificationStatus?:"",
             map.occurrenceRemarks?:"",
             map.coordinateUncertaintyInMeters?:"",
             map.geodeticDatum?:"",
             map.imageLicence?:"",
             map.locality?:"",
             map.multimedia ? map.multimedia.collect {it.identifier}.join(";") : "",
             it.lastUpdated ? it.lastUpdated.format("dd-MM-yyyy")  : ""
            ] as String[]
          )
        }
        csvWriter.flush()
        csvWriter.close()
    }

    /**
     * JSON body looks like:
     * {
     *  "id":"34234324324"
     *  "addImages":[....]   //array of urls to new images
     *  "removeImages":[...]  //array of urls to existing images
     * }
     */
    def updateImages(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        if (json.id){
            def record = Record.findById(new ObjectId(json.id))
            if (record){
                if(json.addImages){
                   json.addImages.each {
                    def mediaFiles = record['associatedMedia']
                    def createdFile = mediaService.download(record.id.toString(), mediaFiles.length-1, obj)
                    mediaFiles.add createdFile.getAbsolutePath()
                    record['associatedMedia'] = mediaFiles
                   }
                }
                if (json.removeImages){
                   json.removeImages.each {
                    def mediaFiles = record['associatedMedia']
                    //translate the full URL to actual path
                    def imagePath = it.replaceAll(
                            grailsApplication.config.fielddata.mediaUrl,
                            grailsApplication.config.fielddata.mediaDir
                    )
                    mediaFiles.remove(createdFile.getPath())
                    record['associatedMedia'] = mediaFiles
                    mediaService.removeImage(imagePath) //delete original & the derivatives
                   }
                }
                record.save(true)
                response.setContentType("application/json")
                def model = [id: record.id.toString(), images:record['associatedMedia']]
                render model as JSON
            } else {
                response.sendError(404, 'Record ID not recognised. JSON payload must contain "id" element for existing record.')
            }
        } else {
            response.sendError(400, 'No record ID was supplied. JSON payload must contain "id" element.')
        }
    }

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

    def updateById(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        //json.eventDate = new Date().parse("yyyy-MM-dd", json.eventDate)
        //TODO add some data validation....
        Record r = Record.findByOccurrenceID(params.id)
        Map errors = recordService.updateRecord(r,json)
        log.debug "updateById() - errors = ${errors}"
        setResponseHeadersForRecord(response, r)
        //add the errors to the header too
        response.addHeader('errors', (errors as grails.converters.JSON).toString())

        if (errors) {
            response.sendError(400, (errors as JSON).toString())
        } else {
            try {
                broadcastService.sendUpdate(r)
            } catch (Exception e){
                log.error(e.getMessage(), e)
            }

            render r as JSON
        }
    }

    private def setResponseHeadersForRecord(response, record){
        response.addHeader("content-location", grailsApplication.config.grails.serverURL + "/fielddata/record/" + record.id.toString())
        response.addHeader("location", grailsApplication.config.grails.serverURL + "/fielddata/record/" + record.id.toString())
        response.addHeader("entityId", record.id.toString())
    }
}