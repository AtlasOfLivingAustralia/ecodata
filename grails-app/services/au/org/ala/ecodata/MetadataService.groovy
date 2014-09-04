package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
import grails.converters.JSON

import java.text.SimpleDateFormat

class MetadataService {

    private static final String ACTIVE = 'active'
    // The spatial portal returns n/a when the point does not intersect a layer.
    private static final String SPATIAL_PORTAL_NO_MATCH_VALUE = 'n/a'

    def grailsApplication, webService, cacheService, messageSource

    def activitiesModel() {
        return cacheService.get('activities-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + 'activities-model.json'
            JSON.parse(new File(filename).text)
        })
    }

    def activitiesList(programName) {

        def activities = activitiesModel().activities

        if (programName) {
            def program = programModel(programName)
            if (program.activities) {
                activities = activities.findAll{it.type in program.activities}
            }
        }

        // Remove deprecated activities
        activities = activities.findAll {!it.status || it.status == ACTIVE}

        Map byCategory = [:]

        // Group by the activity category field, falling back on a default grouping of activity or assessment.
        activities.each {
            def category = it.category?: it.type == 'Activity' ? 'Activities' : 'Assessment'
            if (!byCategory[category]) {
                byCategory[category] = []
            }
            def description = messageSource.getMessage("api.${it.name}.description", null, "", Locale.default)
            byCategory[category] << [name:it.name, description:description]
        }
        byCategory
    }

    def programsModel() {
        return cacheService.get('programs-model',{
            String filename = (grailsApplication.config.app.external.model.dir as String) + 'programs-model.json'
            JSON.parse(new File(filename).text)
        })
    }

    def programModel(program) {
        return programsModel().programs.find {it.name == program}
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

        def nvisLayers = grailsApplication.config.app.facets.geographic.special

        nvisLayers.each { name, path ->
            def classesJsonFile = new File(path + '.json')
            if (classesJsonFile.exists()) { // The files are too big for the development system
                def classesJson = classesJsonFile.text
                def classesMap = JSON.parse(classesJson)

                BasicGridIntersector intersector = null
                try {
                    intersector = new BasicGridIntersector(path)
                    def classNumber = intersector.readCell(lon, lat)
                    retMap.put(name, classesMap[classNumber.toInteger().toString()])
                } catch (IllegalArgumentException ex) {
                    // Lat long was outside extent of grid
                    retMap.put(name, null)
                }
                finally {
                    if (intersector != null) {
                        intersector.close()
                    }
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

    /**
     * This method produces the location metadata for a point, used in particular to provide the geographic facet terms
     * for a site.  This is done by intersecting the site centroid against a configured set of spatial portal layers
     * and storing the results against attributes that are indexed specifically for faceting.
     * @param lat the latitude of the point.
     * @param lng the longitude of the point.
     * @return metadata for the point.
     */
    def getLocationMetadataForPoint(lat, lng) {

        def features = performLayerIntersect(lat, lng)

        def localityUrl = grailsApplication.config.google.geocode.url + "${lat},${lng}"
        def result = webService.getJson(localityUrl)
        def localityValue = (result?.results && result.results)?result.results[0].formatted_address:''
        features << [locality: localityValue]

        // Return the Nvis classes for the supplied location. This is an interim solution until the spatial portal can be fixed to handle
        // large grid files such as the NVIS grids.
        features << getNvisClassesForPoint(lat as Double, lng as Double)

        features
    }

    /**
     * Reads the facet configuration and intersects the supplied point against the defined facets.
     * @param lat the latitude of the point.
     * @param lng the longitude of the point.
     * @return metadata for the point obtained from the spatial portal.
     */
    def performLayerIntersect(lat,lng) {


        def griddedLayers = grailsApplication.config.app.facets.geographic.gridded
        def groupedFacets = grailsApplication.config.app.facets.geographic.grouped

        // Extract all of the layer field ids from the facet configuration so we can make a single web service call to the spatial portal.
        def fieldIds = griddedLayers.collect { k, v -> v }
        groupedFacets.each { k, v ->
            fieldIds.addAll(v.collect { k1, v1 -> v1 })
        }

        // Do the intersect
        def featuresUrl = grailsApplication.config.spatial.intersectUrl + "${fieldIds.join(',')}/${lat}/${lng}"
        def features = webService.getJson(featuresUrl)

        def facetTerms = [:]
        griddedLayers.each { name, fid ->
            def match = features.find { it.field == fid }
            if (match  && match.value != SPATIAL_PORTAL_NO_MATCH_VALUE) {
                facetTerms << [(name): match.value]
            }
        }

        groupedFacets.each { group, layers ->
            def groupTerms = []
            layers.each { name, fid ->
                def match = features.find { it.field == fid }
                if (match && match.value != SPATIAL_PORTAL_NO_MATCH_VALUE) {
                    groupTerms << match.value
                }
            }
            if (groupTerms) {
                facetTerms << [(group): groupTerms]
            }
        }

        facetTerms
    }

}
