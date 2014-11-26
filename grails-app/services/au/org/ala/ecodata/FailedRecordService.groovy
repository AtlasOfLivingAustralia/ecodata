package au.org.ala.ecodata

class FailedRecordService {


    def markAsFailed(type,record){
        log.debug("Marking as failed")
        FailedRecord fr = FailedRecord.findByRecord(record)?:new FailedRecord()
        log.debug(fr.toString() + " " + fr.id.toString() )
        fr.lastAttempted = new Date()
        fr.numberOfAttempts=fr.numberOfAttempts != null? fr.numberOfAttempts + 1:1
        fr.updateType =type
        fr.record = record
        fr.save(true)
        log.debug(fr.toString() + " " + fr.properties)
    }

    def removeFailed(record){
        log.debug("Removing the failed record")
        FailedRecord fr = FailedRecord.findByRecord(record)
        fr?.delete()
    }

    def toMap(record){
        def dbo = record.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")

        def recordId = mapOfProperties["record"].getId().toString()
        mapOfProperties.remove("record")
        mapOfProperties["record"] = recordId

        mapOfProperties
    }
}
