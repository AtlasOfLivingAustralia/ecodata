package au.org.ala.ecodata

class RecordService {

    def grailsApplication

    def serviceMethod() {}

    final def ignores = ["action","controller","associatedMedia"]

    def mediaService

    def broadcastService

    def userFielddataService

    def createRecord(json){
        Record r = new Record()
        r = r.save(true)
        def errors = updateRecord(r,json)
        //download the supplied images......
        if(errors.size() ==0){
            try{
                broadcastService.sendCreate(r)
            }
            catch(Exception e){
                //TODO a retry mechanism...
                log.error(e.getMessage(), e)
            }
        }
        [r,errors]
    }

    def addImageToRecord(Record record, String filename, byte[] imageAsByteArray){
        File createdFile = mediaService.copyBytesToImageDir(record.id.toString(), filename, imageAsByteArray)
        if(record['associatedMedia']){
            record['associatedMedia'] << createdFile.getPath()
        } else {
            record['associatedMedia'] = createdFile.getPath()
        }
        record.save(true)
    }

    def updateRecord(r, json){
        def errors =[:]
        try{
            json.each {
                if(!ignores.contains(it.key) && it.value){
                    if (it.value && it.value instanceof BigDecimal ){
                        //println "Before: " + it.value
                        r[it.key] = it.value.toString()
                        //println "After: " + r[it.key]
                    } else {
                        r[it.key] = it.value
                    }
                }
            }

            //look for associated media.....
            if (List.isCase(json.associatedMedia)){

                def mediaFiles = []
                def originalFiles = []
                if (r['associatedMedia']) {
                    r['associatedMedia'].each {
                        mediaFiles << it
                        originalFiles << it
                    }
                }

                if(!originalFiles) originalFiles = []

                def originalFilesSuppliedAgain = []

                json.associatedMedia.eachWithIndex() { obj, i ->
                    //are any of these images existing images ?
                    //println "Processing associated media URL : " + obj
                    if (obj.startsWith(grailsApplication.config.fielddata.mediaUrl)){
                        //URL already loaded - do nothing
                      //  println("URL already loaded: " + obj)
                        def imagePath = grailsApplication.config.fielddata.mediaDir + (obj - grailsApplication.config.fielddata.mediaUrl)
                       // println("URL already loaded - transformed image path: " + imagePath)
                        originalFilesSuppliedAgain <<  imagePath
                    } else {
                       // println("URL NOT loaded. Downloading file: " + obj)
                        try{
                            def createdFile = mediaService.download(r.id.toString(), i, obj)
                            mediaFiles << createdFile.getPath()
                        }
                        catch(Exception e){
                            log.error(e.getMessage(), e)
                            errors[obj] = e.getClass().toString() +" " +e.getMessage()
                        }
                    }
                }

                //do we need to delete any files ?
                originalFiles.each {
                    if(!originalFilesSuppliedAgain.contains(it)){
                        log.info("Removing :" + it)
                        mediaFiles.remove(it)
                    }
                }

                r['associatedMedia'] = mediaFiles
            } else if(json.associatedMedia) {
                try{
                    def createdFile = mediaService.download(r.id.toString(), 0, json.associatedMedia)
                    r['associatedMedia'] = createdFile.getPath()
                }
                catch(Exception e){
                    log.error(e.getMessage(), e)
                    errors[json.associatedMedia] = e.getClass().toString() + " " +e.getMessage();
                }
            }

            if(!r['occurrenceID']){
                r['occurrenceID'] = r.id.toString()
            }

            r.save(flush: true)
        }
        catch(Exception e){
            log.error(e.getMessage(), e)
            //NC catch an unhandled errors so that we don't insert records that have major issues. ie missing userID
            errors['updateError']=   e.getClass().toString() +" " +e.getMessage()
        }
        errors
    }

    def toMap(record){
        def dbo = record.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        //add userDisplayName - Cacheable not working....
        if(mapOfProperties["userId"]){
            def userMap = userFielddataService.getUserNamesForIdsMap()
            def userId = mapOfProperties["userId"]
            def userDisplayName = userMap.get(userId)
            if(userDisplayName){
                 mapOfProperties["userDisplayName"] = userDisplayName
            }
        }
        mediaService.setupMediaUrls(mapOfProperties)
        mapOfProperties
    }
}
