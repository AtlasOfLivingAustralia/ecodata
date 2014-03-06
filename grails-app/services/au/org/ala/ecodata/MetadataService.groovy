package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
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

    def programsModel() {
        return cacheService.get('programs-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + 'programs-model.json'
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

    def getOutputDataModelByName(name) {
        def outputModel = activitiesModel().outputs.find{it.name == name}
        return getOutputDataModel(outputModel?.template)
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

    /**
     * Returns the institution from the institutionList() that matches the supplied
     * name (using a case-insensitive match).
     * @param name the name of the institution to find.
     * @return the institution with the supplied name, or null if it cannot be found.
     */
    def findInstitutionByName(String name) {
        def lowerCaseName = name.toLowerCase()
        return institutionList().find{ it.name.toLowerCase() == lowerCaseName }
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

    def updateProgramsModel(model) {
        writeWithBackup(model, grailsApplication.config.app.external.model.dir, '', 'programs-model', 'json')
        // make sure it gets reloaded
        cacheService.clear('programs-model')
    }

    // Return the Nvis classes for the supplied location. This is an interim solution until the spatial portal can be fixed to handle
    // large grid files such as the NVIS grids.
    def getNvisClassesForPoint(Double lat, Double lon) {
        def retMap = [:]

        def nvisLayerNames = grailsApplication.config.app.nvis_grids.names.toString().split(",")

        for(name in nvisLayerNames) {
            def classesJsonFile = new File(grailsApplication.config.app.nvis_grids.location.toString() + '/' + name + '.json')
            if (classesJsonFile.exists()) { // The files are too big for the development system
                def classesJson = classesJsonFile.text
                def classesMap = JSON.parse(classesJson)

                try {
                    BasicGridIntersector intersector = new BasicGridIntersector(grailsApplication.config.app.nvis_grids.location.toString() + '/' + name)
                    def classNumber = intersector.readCell(lon, lat)
                    retMap.put(name, classesMap[classNumber.toInteger().toString()])
                } catch (IllegalArgumentException ex) {
                    // Lat long was outside extent of grid
                    retMap.put(name, null)
                }
            }
            else {
                retMap << [(name) : null]
            }

        }

        return retMap
    }

    /**
     * Attaches a label matching the form to each entry of the dataModel in the output model template for the
     * specified output.
     * @param outputName identifies the output to annotate.
     */
    def annotatedOutputDataModel(outputName) {
        def outputMetadata = getOutputDataModelByName(outputName)

        def annotatedModel = null
        if (outputMetadata) {
            OutputMetadata metadata = new OutputMetadata(outputMetadata)
            annotatedModel = metadata.annotateDataModel()
        }
        annotatedModel
    }

}
