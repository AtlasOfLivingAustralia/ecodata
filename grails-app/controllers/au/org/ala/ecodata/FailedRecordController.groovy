package au.org.ala.ecodata

import grails.converters.JSON

class FailedRecordController {

    def failedRecordService

    def asJson = { model ->
        response.setContentType("application/json; charset=\"UTF-8\"")
        model
    }

    def list() {
        log.debug("list request....")
        def records = []
        def sort = params.sort ?: "id"
        def order = params.order ?:  "asc"
        def offset = params.start ?: 0
        def max = params.pageSize ?: 10
        FailedRecord.list([sort:sort,order:order,offset:offset,max:max]).each{
            records.add(failedRecordService.toMap(it))
        }
        response.setContentType("application/json")
        def model = [records: records]
        render model as JSON
    }

    def id(){
        FailedRecord fr = FailedRecord.get(params.id)?:FailedRecord.findByRecord(Record.get(params.id))
        if(fr){
            response.setContentType("application/json")
            [failed: failedRecordService.toMap(fr)]
        } else {
            response.sendError(404, 'Unrecognised Failed Record. This record may have been removed.')
        }
    }

}
