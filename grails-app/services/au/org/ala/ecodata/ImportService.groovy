package au.org.ala.ecodata

import groovy.json.JsonSlurper
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import org.apache.commons.lang.StringUtils

class ImportService {

    def mediaService

    def serviceMethod() {}

    def linkWithBiocache(){

        def js  = new JsonSlurper()
        Record.findAll().each { record ->
           def response = new URL("http://biocache.ala.org.au/ws/occurrences/search?facet=off&q="+ URLEncoder.emcode(record.occurrenceID)).text
           def json = js.parseText(response)
           if(json.totalRecords == 1){
               //we have a match

               //get the UUID

               //get the image references


           }
        }
    }

    def loadFile(filePath, reloadImages){
        def columns = [] as List
        println "Starting import of data....."
        String[] dateFormats = ["yyyy-MM-dd HH:mm:ss.s", "yyyy-MM-dd HH:mm:ssZ", "dd/MM/yyyy","yyyy-MM-dd", "dd/MM/yy"]

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
                    if(obj == "occurrenceID")
                       indexOfOccurrenceID = i
                    if(obj == "associatedMedia")
                        associatedMediaIdx = i
                }
            } else {

                def preloaded = false
                Record record = null

                //is record already loaded ?
                if(indexOfOccurrenceID >= 0){
                    record = Record.findWhere([occurrenceID:it[indexOfOccurrenceID]])
                    preloaded = (record != null)
                }

                if(!record){
                    record = new Record()
                }

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
                                //record.eventTime = eventTimeFormatted
                            } catch (Exception e) {
                                e.printStackTrace()
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
                                    println("Problem parsing modified: " + column)
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

                    if(!preloaded || reloadImages){
                        if(associatedMediaIdx >= 0 && it[associatedMediaIdx]){
                            try {
                                def mediaFile = mediaService.copyToImageDir(record.id.toString(), it[associatedMediaIdx])
                                println "Media filepath: " + mediaFile.getPath()
                                if(mediaFile){
                                    record['associatedMedia'] = mediaFile.getPath()
                                    record.save(flush:true)
                                } else {
                                    println "Unable to import media for path: " +  it[associatedMediaIdx]
                                }
                            } catch(Exception e){
                                println("Error loading images: " + it[associatedMediaIdx])
                                record['associatedMedia'] = null
                                record.save(flush:true)
                            }
                        }
                    }
                }
            }
        }
        println "Total loaded: " + count
    }
}
