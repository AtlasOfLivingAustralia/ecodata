package au.org.ala.ecodata

import groovy.json.JsonSlurper
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat

/**
 * Service for importing darwin core CSV records into the system.
 *
 * This controller was written to aid data migration from fielddata into ecodata.
 * Once this is done, this class and the accompanying service should probably
 * be removed or generalised.
 */
class RecordImportService {

    def userService

    def grailsApplication

    def serviceMethod() {}

    def linkWithAuth(){
        def count = 0
        def images = 0
        def userLookupCache = [:]

        Record.findAll().each { record ->
            try {
                count++
                log.info("[record ${count}]  Syncing auth for: " + record.occurrenceID)
                def userDetails = userLookupCache.get(record.userId)
                if (!userDetails) {
                    userDetails = userService.getUserForUserId(record.userId)
                    userLookupCache.put(record.userId, userDetails)
                }
                record.userDisplayName = userDetails.displayName
                record.save(flush: true)
            } catch (Exception e){
                log.error("Problem syncing record: " + record.occurrenceID, e)
            }
        }
        log.info("Finished linking auth and image information. Count ${count}")
        [count: count, images: images]
    }

    def linkWithImages(){
        def count = 0
        def images = 0

        def js  = new JsonSlurper()

        Record.findAll().each { record ->

           try {
               def url = grailsApplication.config.biocacheService.baseURL + "/occurrences/search?facet=off&q=occurrence_id:\"" + record.occurrenceID + "\""
               log.info("[record ${count}] Retrieving from biocache: ${url}")
               def response = new URL(url).text
               def json = js.parseText(response)
               if (json.totalRecords == 1) {
                   count++
                   //get the image references
                   if (json.occurrences[0].imageUrls) {
                       images++
                       record.multimedia = []
                       json.occurrences[0].imageUrls.each {
                           def imageId = it.substring(it.indexOf("=") + 1)
                           def imageUrl = grailsApplication.config.imagesService.baseURL + "/ws/getImageInfo?id=" + imageId
                           log.info("[images ${images}] Retrieving from images: " + imageUrl)
                           def imageMetadata = js.parseText(new URL(imageUrl).text)
                           record.multimedia << [
                                   created   : imageMetadata.dateUploaded,      //image service
                                   title     : imageMetadata.originalFileName,  //image service
                                   format    : imageMetadata.mimeType,          //image service
                                   creator   : record.userDisplayName,         //CAS
                                   rightsHolder: record.userDisplayName,       //CAS
                                   license   : record.imageLicence ?: "Creative Commons Attribution",
                                   type      : "StillImage",
                                   imageId   : imageId,
                                   identifier: it
                           ]
                       }
                   }
                   record.save(flush: true)
               }
           } catch (Exception e){
               log.error("Problem syncing record: " + record.occurrenceID)
           }
        }
        log.info("Finished linking auth and image information. Count ${count}, Images ${images}")
        [count: count, images: images]
    }

    def loadFile(filePath, projectId){

        log.info "Starting import of data....."

        def columns = [] as List
        def count = 0
        def imported = 0
        def indexOfOccurrenceID = -1
        def associatedMediaIdx = -1

        new File(filePath).eachCsvLine {
            count += 1
            //println "Starting....." + count
            if(count == 1){
                // header row (column titles)
                columns = it as List
                columns.eachWithIndex { obj, i ->

                    if(obj == "occurrenceID"){
                        indexOfOccurrenceID = i
                    }

                    if(obj == "associatedMedia"){
                        associatedMediaIdx = i
                    }

                }
            } else {

                Record record = null

                //is record already loaded ?
                if(indexOfOccurrenceID >= 0){
                    def occurrenceID = it[indexOfOccurrenceID]
                    record = Record.findWhere([occurrenceID: occurrenceID])
                }

                if(!record){
                    record = new Record()
                }

                //set project Id for this data....
                record.projectId = projectId

                def dynamicPropertiesToAdd = [:]

                it.eachWithIndex { column, idx ->
                    log.trace("Field debug : " + columns[idx] + " : " + column)
                    if(column && column.trim() != "" && columns[idx] != "associatedMedia" && columns[idx] != "eventTime") {
                        if(columns[idx] == "createdDate" && column){
                           try {
                                //2012-02-15 17:20:00.0
                                record.dateCreated = DateUtils.parseDate(column,["yyyy-MM-dd HH:mm:ss.SSS"].toArray(new String[0]))
                                record.lastUpdated = DateUtils.parseDate(column,["yyyy-MM-dd HH:mm:ss.SSS"].toArray(new String[0]))
                           } catch (Exception e) {
                               println("Problem parsing:" + column)
                           }
                        } else if(columns[idx] == "eventDate" && column){
                            try {
                                def dateTimeString = column

                                def timeString = it[columns.indexOf("eventTime")] // will be HH:MM

                                if (timeString && timeString.size() == 5) {
                                    dateTimeString += "T" + timeString + ":00 +1000" // assume EST (TZ not captured by sightings)
                                } else {
                                    dateTimeString += "T00:00:00 +1000" // assume EST (TZ not captured by sightings)
                                }

                                def suppliedDate = DateUtils.parseDate(dateTimeString, ["yyyy-MM-dd'T'HH:mm:ss Z"] as String[])
                                SimpleDateFormat utcf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") //get UTC
                                utcf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                record.eventDate = utcf.format(suppliedDate)
                            } catch (Exception e) {
                                log.error("Problem importing record")
                            }
                        } else if(columns[idx] == "decimalLatitude"){
                            if(column && column != "null"){
                                try {
                                    record[columns[idx]] = Float.parseFloat(column)
                                } catch(NumberFormatException e){
                                    record["verbatimLatitude"] = column
                                }
                            }
                        } else if(columns[idx] == "decimalLongitude"){
                            if(column && column != "null"){
                                try {
                                    record[columns[idx]] = Float.parseFloat(column)
                                } catch(NumberFormatException e){
                                    record["verbatimLongitude"] = column
                                }
                            }
                        } else if(columns[idx] == "coordinateUncertaintyInMeters"){
                            if(column && column != "null"){
                                def value = "${column}"
                                if (!column.isInteger() && column.isFloat()) {
                                    // some values come in as "10.0"
                                    value = column.toFloat()?.trunc().toInteger()
                                    record[columns[idx]] = value
                                } else {
                                    try {
                                        record[columns[idx]] = Integer.parseInt(value)
                                    } catch (Exception e){
                                        //ignored
                                    }
                                }
                            }
                        } else if(columns[idx] == "individualCount"){
                            if(column && column != "null"){
                                try {
                                    record[columns[idx]] = Integer.parseInt(column.replaceAll("[^0-9]", ""))
                                } catch (Exception e){
                                    //ignored
                                }
                            }
                        } else if(columns[idx] == "userId"){
                            record.userId = column
                        } else if(columns[idx] == "modified"){
                            if(column && column != "null"){
                                try {
                                    // 15-02-2012 format
                                    record.dateCreated = DateUtils.parseDate(column,["dd-MM-yyyy"].toArray(new String[0]))
                                    record.lastUpdated = DateUtils.parseDate(column,["dd-MM-yyyy"].toArray(new String[0]))
                                } catch (Exception e) {
                                    log.error("Problem parsing modified dates: " + column)
                                }
                            }
                        } else if(columns[idx] == "dateCreated"){

                        } else {
                            dynamicPropertiesToAdd.put(columns[idx],  column)
                        }
                    }
                }

                if(record.userId){
                    record = record.save(flush: true)
                    //add dynamic properties
                    dynamicPropertiesToAdd.each { k, v -> record."${k}" = v }
                    record.save(flush: true)
                    imported ++
                    log.info("Importing record: " + record.id + ", count: " + count + ", imported: " + imported + ", skipped: " + (count-imported))
                }
            }
        }
        println "Total loaded: " + count
    }
}
