package au.org.ala.ecodata

import grails.converters.JSON

import static groovyx.gpars.actor.Actors.actor

class ImportController {

    def importService

    def index() { }

    def importFile(){

       def filePath = params.filePath
       def reloadImages = params.reloadImages

       def theActor = actor {
            println "Starting a thread.....reload images: " + reloadImages
            importService.loadFile(filePath, reloadImages)
            println "Finishing thread."
       }

       response.setContentType("application/json")
       def model = [started:true]
       render model as JSON
    }

}
