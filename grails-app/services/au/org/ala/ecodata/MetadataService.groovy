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

    def getOutputDataModel(templateName) {
        return cacheService.get(templateName + '-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + templateName + '/dataModel.json'
            JSON.parse(new File(filename).text)
        })
    }

    def updateOutputDataModel(model, templateName) {
        log.debug "updating template name = ${templateName}"
        writeWithBackup(model, grailsApplication.config.app.external.model.dir, templateName, 'dataModel', 'json')
        // make sure it gets reloaded
        cacheService.clear(templateName + '-model')
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

    def writeWithBackup(content, modelPathRoot, path, filename, extension) {
        /* build path creating directories as required */
        // assume root ends with a path separator
        String filePath = modelPathRoot + path + (path.endsWith('/') ? '' : '/')
        // get new or existing file
        def f = new File(filePath + filename + '.' + extension)

        // create dirs as required
        new File(f.getParentFile().getAbsolutePath()).mkdirs()

        if (f.exists()) {
            // create a backup of the file appending the current timestamp to the name
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            def backupFilename = filePath + filename + "-" + sdf.format(date) + '.' + extension
            f.renameTo(new File(backupFilename))
        }

        // write the new content
        f.withWriter { out ->
            out.write content as String
        }
    }

    def updateActivitiesModel(model) {
        writeWithBackup(model, grailsApplication.config.app.external.model.dir, '', 'activities-model', 'json')
        // make sure it gets reloaded
        cacheService.clear('activities-model')
    }

}
