package au.org.ala.ecodata

import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import org.apache.commons.lang.StringUtils

class ImportService {

    def mediaService

    def serviceMethod() {}

    def loadFile(filePath, reloadImages){
        def columns = []
        println "Starting import of data....."
        String[] dateFormats = ["yyyy-MM-dd HH:mm:ss.s", "dd/MM/yyyy"]

        def count = 0
        def imported = 0
        def indexOfOccurrenceID = -1
        def associatedMediaIdx = -1

        new File(filePath).eachCsvLine {
            count += 1
            //println "Starting....." + count
            if(count == 1){
                columns = it
                columns.eachWithIndex { obj, i ->
                    if(obj == "occurrenceID")
                       indexOfOccurrenceID = i
                    if(obj == "associatedMedia")
                        associatedMediaIdx = i
                }
            } else {

                def preloaded = false
                Record r = null
                if(indexOfOccurrenceID >=0){
                    //is record already loaded ?
                    r = Record.findWhere([occurrenceID:it[indexOfOccurrenceID]])
                    preloaded = (r != null)
                }

                if(!r){
                    r = new Record()
                }

                it.eachWithIndex { column, idx ->
                    log.trace("Field debug : " + columns[idx] + " : " + column)
                    if(column && column.trim() != "" && columns[idx] != "associatedMedia" && columns[idx] != "eventTime") {
                        if(columns[idx] == "createdDate" && column){
                           try {
                                //2012-02-15 17:20:00.0
                                r.dateCreated = DateUtils.parseDate(column,["yyyy-MM-dd HH:mm:ss.SSS"].toArray(new String[0]))
                                r.lastUpdated = DateUtils.parseDate(column,["yyyy-MM-dd HH:mm:ss.SSS"].toArray(new String[0]))
                           } catch (Exception e) {
                               println("Problem parsing:" + column)
                           }
                        } else if(columns[idx] == "eventDate" && column){
                            try {
                                def suppliedDate = DateUtils.parseDate(column, dateFormats)
                                SimpleDateFormat yyymmdd = new SimpleDateFormat("yyyy-MM-dd")
                                SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm")
                                r[columns[idx]] = yyymmdd.format(suppliedDate)
                                def eventTimeFormatted = hhmm.format(suppliedDate)
                                r.eventTime = eventTimeFormatted
                            } catch (Exception e) {}
                        } else if(columns[idx] == "decimalLatitude"){
                            if(column && column != "null"){
                                r[columns[idx]] = Float.parseFloat(column)
                            }
                        } else if(columns[idx] == "decimalLongitude"){
                            if(column && column != "null"){
                                r[columns[idx]] = Float.parseFloat(column)
                            }
                        } else {
                            r[columns[idx]] = column
                        }
                    }
                }
                r = r.save(flush: true)

                imported ++
                log.info("Importing record: " + r.id + ", count: " + count + ", imported: " + imported + ", skipped: " + (count-imported))

                if(!preloaded || reloadImages){
                    if(associatedMediaIdx>=0 && it[associatedMediaIdx]){
                        try {
                            def mediaFile = mediaService.copyToImageDir(r.id.toString(), it[associatedMediaIdx])
                            println "Media filepath: " + mediaFile.getPath()
                            if(mediaFile){
                                r['associatedMedia'] = mediaFile.getPath()
                                r.save(flush:true)
                            } else {
                                println "Unable to import media for path: " +  it[associatedMediaIdx]
                            }
                        } catch(Exception e){
                            println("Error loading images: " + it[associatedMediaIdx])
                            r['associatedMedia'] = null
                            r.save(flush:true)
                        }
                    }
                }
            }
        }
        println "Total loaded: " + count
    }
}
