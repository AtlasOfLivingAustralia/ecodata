package au.org.ala.ecodata

import grails.converters.JSON

import static groovyx.gpars.actor.Actors.actor

class ImportController {

    def importService

    static defaultAction = "importFile"

    def importFile(){

        def model = [:]

       if(params.filePath && new File(params.filePath).exists()){
           def filePath = params.filePath
           def reloadImages = params.reloadImages

           def theActor = actor {
               println "Starting a thread.....reload images: " + reloadImages
               importService.loadFile(filePath, reloadImages)
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
