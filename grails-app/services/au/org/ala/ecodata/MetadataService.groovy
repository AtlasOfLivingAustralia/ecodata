package au.org.ala.ecodata

import grails.converters.JSON

import java.text.SimpleDateFormat

class MetadataService {

    def grailsApplication, webService, cacheService

    def activitiesModel() {
        return cacheService.get('activities-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + 'activities-model.json'
            JSON.parse(new File(filename).text)
        })
    }

    def getOutputModel(name) {
        return activitiesModel().outputs.find { it.name == name }
    }

    def getDataModel(name) {
        return cacheService.get(name + '-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + name + '/dataModel.json'
            JSON.parse(new File(filename).text)
        })
    }

    def getModelName(output, type) {
        return output.template ?: getModelNameFromType(type)
    }

    def getModelNameFromType(type) {
        //log.debug "Getting model name for ${type}"
        return activitiesModel().find({it.name == type})?.template
    }

    def getInstitutionName(uid) {
        return uid ? institutionList().find({ it.uid == uid })?.name : ''
    }

    def institutionList() {
        return cacheService.get('institutions',{
            webService.getJson(grailsApplication.config.collectory.baseURL + '/ws/institution')
        })
    }

    def updateActivitiesModel(model) {
        // get the existing model
        String filename = (grailsApplication.config.app.external.model.dir as String) + 'activities-model.json'
        def f = new File(filename)

        // create a backup of the file appending the current timestamp to the name
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        def backupFilename = (grailsApplication.config.app.external.model.dir as String) + 'activities-model' +
                "-" + sdf.format(date) + ".json"
        f.renameTo(new File(backupFilename))

        // write the new model
        f.write(model)

        // make sure it gets reloaded
        cacheService.clear('activities-model')
    }
}
