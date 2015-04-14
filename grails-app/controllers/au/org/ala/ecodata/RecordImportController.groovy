package au.org.ala.ecodata

import grails.converters.JSON

import static groovyx.gpars.actor.Actors.actor

/**
 * Controller for importing darwin core CSV records into the system.
 *
 * This controller was written to aid data migration from fielddata into ecodata.
 * Once this is done, this class and the accompanying service should probably
 * be removed or generalised.
 */
class RecordImportController {

    def recordImportService

    static defaultAction = "importFile"

    def linkWithAuth(){
        def theActor = actor {
            recordImportService.linkWithAuth()
        }
        def model = [started:true]
        render model as JSON
    }

    def linkWithImages(){
        def theActor = actor {
            recordImportService.linkWithImages()
        }
        def model = [started:true]
        render model as JSON
    }

    def importFile(){

       def model = [:]

       if(params.filePath && new File(params.filePath).exists()){
           def filePath = params.filePath
           def theActor = actor {
               println "Starting a thread....."
               recordImportService.loadFile(filePath)
               println "Finishing thread."
           }
           model = [started:true]
       } else {
           model = [started:false, reason:"please supply a file"]
       }

       response.setContentType("application/json")

       render model as JSON
    }
}