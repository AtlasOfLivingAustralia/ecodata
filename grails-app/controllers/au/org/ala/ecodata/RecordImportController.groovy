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
        actor {
            recordImportService.linkWithAuth()
        }
        def model = [started:true]
        render model as JSON
    }

    def linkWithImages(){
        actor {
            recordImportService.linkWithImages()
        }
        def model = [started:true]
        render model as JSON
    }

    def importFromUrl(){
        def model = [:]

        def url = params.url
        def projectId = params.projectId

        if(projectId && url){

            def project = Project.findByProjectId(projectId)
            if(project){

                def tempOutputFile = "/tmp/import-${System.currentTimeMillis()}.csv"
                if(url){
                    new File(tempOutputFile).withOutputStream { out ->
                        new URL(url).withInputStream { from ->  out << from; }
                    }
                    actor {
                        println "Starting a thread....."
                        recordImportService.loadFile(tempOutputFile, projectId)
                        println "Finishing thread."
                    }
                    model = [started:true]
                } else {
                    model = [started:false, reason:"please supply a file"]
                }

            } else {

                model = [started:false, reason:"Invalid 'projectId' parameter."]
            }

        } else {
            model = [started:false, reason:"Please supply a 'url' and 'projectId' parameter."]
        }
        response.setContentType("application/json")

        render model as JSON
    }

    def importFile(){

       def model = [:]

       def filePath = params.filePath
       def projectId = params.projectId

       if(filePath && new File(filePath).exists()){
           actor {
               println "Starting a thread....."
               recordImportService.loadFile(filePath, projectId)
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