package au.org.ala.ecodata

import au.com.bytecode.opencsv.CSVWriter
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringEscapeUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.DefaultHttpClient

/**
 * Services for handling the creation of records with images.
 */
class RecordService {

    def grailsApplication
    def activityService, metadataService, outputService, projectService, siteService


    def serviceMethod() {}

    final def ignores = ["action","controller","associatedMedia"]

    /**
     * Export records to CSV.
     */
    def exportCSV(OutputStream outputStream, Date cutOffDate = null){
        def csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))
        csvWriter.writeNext(
                [
                        "occurrenceID",
                        "scientificName",
                        "family",
                        "kingdom",
                        "decimalLatitude",
                        "decimalLongitude",
                        "eventDate",
                        "userId",
                        "recordedBy",
                        "usingReverseGeocodedLocality",
                        "individualCount",
                        "submissionMethod",
                        "georeferenceProtocol",
                        "identificationVerificationStatus",
                        "occurrenceRemarks",
                        "coordinateUncertaintyInMeters",
                        "geodeticDatum",
                        "imageLicence",
                        "locality",
                        "associatedMedia",
                        "modified",
                ] as String[]
        )

        def recordList = null

        if(cutOffDate){
            recordList = Record.where { lastUpdated >= cutOffDate }
        } else {
            recordList =  Record.list()
        }

        recordList.each {
            def map = toMap(it)
            csvWriter.writeNext(
                    [
                            map.occurrenceID?:"",
                            map.scientificName?:"",
                            map.family?:"",
                            map.kingdom?:"",
                            map.decimalLatitude?:"",
                            map.decimalLongitude?:"",
                            map.eventDate?:"",
                            map.userId?:"",
                            map.recordedBy?:"",
                            map.usingReverseGeocodedLocality?:"",
                            map.individualCount?:"",
                            map.submissionMethod?:"",
                            map.georeferenceProtocol?:"",
                            map.identificationVerificationStatus?:"",
                            map.occurrenceRemarks?:"",
                            map.coordinateUncertaintyInMeters?:"",
                            map.geodeticDatum?:"",
                            map.imageLicence?:"",
                            map.locality?:"",
                            map.multimedia ? map.multimedia.collect {it.identifier}.join(";") : "",
                            it.lastUpdated ? it.lastUpdated.format("dd-MM-yyyy")  : ""
                    ] as String[]
            )
        }
        csvWriter.flush()
        csvWriter.close()
    }

    /**
     * Create a record based on the supplied JSON
     *
     * @param json
     * @return
     */
    def createRecord(json){
        Record record = new Record().save(true)
        def errors = updateRecord(record,json)
        [record, errors]
    }

    /**
     * Update the supplied record including updates to any supplied images.
     *
     * @param record
     * @param json
     * @return
     */
    private def updateRecord(Record record,  json){
        def errors = [:]
        try {
            json.each {
                if(!ignores.contains(it.key) && it.value){
                    record[it.key] = it.value
                }
            }

            //persist an images into image service
            if(json.multimedia){
                json.multimedia.eachWithIndex { image, idx ->
                    // Only upload images that are NOT already in images.ala.org.au
                    if (!image.identifier.contains(grailsApplication.config.imagesService.baseURL)) {
                        def address = image.identifier ? image.identifier : image.url
                        def downloadedFile = download(record.occurrenceID, idx, address)
                        log.debug "Uploading image - ${address}"
                        def imageId = uploadImage(record, downloadedFile, image)
                        record.multimedia[idx].imageId = imageId
                        record.multimedia[idx].identifier = grailsApplication.config.imagesService.baseURL + "/image/proxyImageThumbnailLarge?imageId=" + imageId
                    }
                }
            }
            record.save(flush: true)
        } catch(Exception e) {
            log.error(e.getMessage(), e)
            //NC catch an unhandled errors so that we don't insert records that have major issues. ie missing userID
            errors['updateError'] = e.getClass().toString() +" " +e.getMessage()
        }
        errors
    }

    /**
     * Upload the supplied image to the image service.
     *
     * @param record
     * @param fileToUpload
     * @param image
     * @return
     */
    private def uploadImage (Record record, File fileToUpload, image) {

        def remoteImageRepo = grailsApplication.config.imagesService.baseURL
        //upload an image
        def httpClient = new DefaultHttpClient()
        def entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
        if (!fileToUpload.exists()) {
            log.error("File to upload does not exist or can not be read. " + fileToUpload.getAbsolutePath())
        } else {
            log.debug("File to upload: " + fileToUpload.getAbsolutePath() + ", size:" + fileToUpload.length())
        }
        def fileBody = new FileBody(fileToUpload, "image/jpeg")
        log.debug "image upload: ${image}"
        entity.addPart("image", fileBody)
        entity.addPart("metadata", new StringBody(([
                "occurrenceId": record.occurrenceID,
                "license": image?.license,
                "copyright": image?.license,
                "originalFilename": image?.title,
                "attribution": image?.creator,
                "dateTaken": image?.created,
                "systemSupplier": grailsApplication.config.imageSystemSupplier?:"ecodata"
        ] as JSON).toString()))

        if (record.tags) {
            entity.addPart("tags", new StringBody((record.tags as JSON).toString()))
        }

        def httpPost = new HttpPost(remoteImageRepo + "/ws/uploadImage")
        httpPost.setEntity(entity)
        httpPost.addHeader("X-ALA-userId","${record.userId}");
        def response = httpClient.execute(httpPost)
        def result = response.getStatusLine()
        def responseBody = response.getEntity().getContent().getText()
        log.debug("Image service response code: " + result.getStatusCode())

        def jsonSlurper = new JsonSlurper()

        def map = jsonSlurper.parseText(responseBody)
        log.debug("Image ID: " + map["imageId"])
        if(!map["imageId"]){
            log.error("Problem uploading images. Response: " + map)
        }
        map["imageId"]
    }

    private File download(recordId, idx, address){
        def directory = grailsApplication.config.app.file.upload.path + File.separator + "record" + File.separator  + recordId
        File mediaDir = new File(directory)
        if (!mediaDir.exists()){
            FileUtils.forceMkdir(mediaDir)
        }
        def destFile = new File(directory + File.separator + idx + "_" + address.tokenize("/")[-1])
        def out = new BufferedOutputStream(new FileOutputStream(destFile))
        log.debug("Trying to download..." + address)
        String decodedAddress = StringEscapeUtils.unescapeXml(address)
        log.debug("Decoded address " + decodedAddress)
        out << new URL(decodedAddress).openStream()
        out.close()
        destFile
    }

    def toMap(record){
        def dbo = record.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        mapOfProperties.remove("_id")
        mapOfProperties
    }


    /**
     * Export project sightings to CSV.
     */
    def exportCSVProject(OutputStream outputStream, projectId, modelName){
        def csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))
        csvWriter.writeNext(
                [
                        "occurrenceID",
                        "scientificName",
                        "family",
                        "kingdom",
                        "decimalLatitude",
                        "decimalLongitude",
                        "eventDate",
                        "userId",
                        "recordedBy",
                        "usingReverseGeocodedLocality",
                        "individualCount",
                        "submissionMethod",
                        "georeferenceProtocol",
                        "identificationVerificationStatus",
                        "occurrenceRemarks",
                        "coordinateUncertaintyInMeters",
                        "geodeticDatum",
                        "imageLicence",
                        "locality",
                        "associatedMedia",
                        "modified",
                        "locationID"
                ] as String[]
        )

        def modelsMap = [:] // cache of output models by name
        def projects
        if (projectId)
            projects = [projectService.get(projectId, projectService.FLAT)]
        else
            projects = projectService.search([:], projectService.FLAT)
        for (def project in projects) {
            if (project) exportOneProject(csvWriter, project, modelsMap, modelName)
        }

        csvWriter.flush()
        csvWriter.close()
    }

    private def exportOneProject(csvWriter, project, modelsMap, modelName) {
        def projectSite = siteService.get(project.projectSiteId)
        def today = (new Date()).format("dd-MM-yyyy")
        def projectDates = mapDates(today, project.plannedStartDate, project.plannedEndDate, project.actualStartDate, project.actualEndDate)
        for (def activity in activityService.findAllForProjectId(project.projectId, activityService.FLAT)) {
            def site, activityDWC = [:]

            // map collector to activity record
            if (activity.collector) activityDWC.userId = activity.collector

            // map site info to activity record
            if (activity.siteId) site = siteService.get(activity.siteId)
            if (!site) site = projectSite
            if (site) mapSiteToDWC(activityDWC, site)

            // map date info to activity record
            activityDWC.eventDate = mapDates(today, activity.plannedStartDate, activity.plannedEndDate, activity.startDate, activity.endDate)
            if (!activityDWC.eventDate) activityDWC.eventDate = projectDates

            // map each output to a record
            def outputs
            if (modelName)
                outputs = outputService.findAllForActivityIdAndName(activity.activityId, modelName)
            else
                outputs = outputService.findAllForActivityId(activity.activityId)
            for (def output in outputs) {
                def model = modelsMap[output.name]
                if (!model) model = modelsMap[output.name] = metadataService.getOutputDataModelByName(output.name)
                if (!model || !model.darwinCore) continue
                def dwc = activityDWC.clone()
                if (output.dateCreated) dwc.eventDate = output.dateCreated.format("dd-MM-yyyy")
                exportOutputTree(csvWriter, dwc, output.data, model.dataModel)
            }
        }
    }

    private def mapSiteToDWC(dwc, site) {
        def extent = site.extent, geom = extent?.geometry
        if (!geom) return // can't do much with this site
        if (extent.source == "pid" && geom.centre?.size() != 2 && site.poi?.size() > 0)
            geom = site.poi[0].geometry
        if (geom.locality) dwc.locality = geom.locality
        if (geom.uncertainty) dwc.coordinateUncertaintyInMeters = geom.uncertainty
        if (site.siteId) dwc.locationID = site.siteId
        def centre = geom.centre
        if (centre && centre.size() == 2) {
            dwc.decimalLatitude = centre[1]
            dwc.decimalLongitude = centre[0]
        }
    }

    private def mapDates(today, plannedStart, plannedEnd, actualStart, actualEnd) {
        def dStart = actualStart?: plannedStart
        def dEnd = actualEnd?: plannedEnd
        if (dStart && dEnd) return dStart.format("dd-MM-yyyy") + ' ' + dEnd.format("dd-MM-yyyy")
        if (dStart) return dStart.format("dd-MM-yyyy") + ' ' + today
        return null
    }

    /*
     * DarwinCore export mappings for project sightings (outputs)
     * (in order of increasing overwrite preference)
     *
     * Project:
     *     site(projectSiteId) {
     *       siteId -> locationId
     *       extent.geometry or poi[0].geometry {
     *         locality -> locality
     *         uncertainty -> coordinateUncertaintyInMeters
     *         centre[0] -> decimalLongitude
     *         centre[1] -> decimalLatitude
     *       }
     *     }
     *     plannedStartDate/actualStartDate plannedEndDate/actualEndDate/today -> eventDate
     *
     * Activity:
     *     site(siteId) -> [overwrite mappings from project site]
     *     plannedStartDate/startDate plannedEndDate/endDate/today -> eventDate
     *     collector -> userId
     *
     * Output: only for model marked "darwinCore: true"
     *     dateCreated -> eventDate
     *     assessmentDate -> eventDate
     *     lastUpdated -> modified
     *     collector -> recordedBy
     *     assessor -> recordedBy
     *     eventNotes -> occurrenceRemarks
     *     notes -> occurrenceRemarks
     *     (dataType:species).name -> scientificName
     *     fields marked "darwinCore: <dwcName>" -> dwcName
     *
     * Output sublist marked "darwinCore: true"
     *     recursive generation of records
     */
    private def exportOutputTree(csvWriter, dwc, output, model) {
        // map leaf-level fields to darwin core
        if (output.assessmentDate) dwc.eventDate = output.assessmentDate.format("dd-MM-yyyy")
        if (output.lastUpdated) dwc.modified = output.lastUpdated.format("dd-MM-yyyy")
        if (output.locality) dwc.locality = output.locality
        if (output.assessor)
            dwc.recordedBy = output.assessor
        else if (output.collector)
            dwc.recordedBy = output.collector
        if (output.notes)
            dwc.occurrenceRemarks = output.notes
        else if (output.eventNotes)
            dwc.occurrenceRemarks = output.eventNotes

        // map species info
        def species = model.find {it.dataType == "species"}
        if (species) dwc.scientificName = output[species.name].name

        // process exlpicit mappings
        model.each {
            def dwcName = it.darwinCore
            if (dwcName) {
                def val = output[it.name]
                if (val) dwc[dwcName] = val
            }
        }

        // determine potential subtree for recursive record generation
        def subtree = model.find {it.dataType == "list" && it.darwinCore}
        if (subtree) {
            output = output[subtree.name]
            if (output && output[0] instanceof Object) {
                model = subtree.columns
                output.each {
                    exportOutputTree(csvWriter, dwc.clone(), it, model)
                }
                return
            }
        }

        // only emit if there is a valid scientificName
        if (!dwc.scientificName || dwc.scientificName.size() == 0) return

        // if no valid subtree was found, we are at a leaf node, so emit it
        csvWriter.writeNext(
                [
                        dwc.occurrenceID?:"",
                        dwc.scientificName?:"",
                        dwc.family?:"",
                        dwc.kingdom?:"",
                        dwc.decimalLatitude?:"",
                        dwc.decimalLongitude?:"",
                        dwc.eventDate?:"",
                        dwc.userId?:"",
                        dwc.recordedBy?:"",
                        dwc.usingReverseGeocodedLocality?:"",
                        dwc.individualCount?:"",
                        dwc.submissionMethod?:"",
                        dwc.georeferenceProtocol?:"",
                        dwc.identificationVerificationStatus?:"",
                        dwc.occurrenceRemarks?:"",
                        dwc.coordinateUncertaintyInMeters?:"",
                        dwc.geodeticDatum?:"",
                        dwc.imageLicence?:"",
                        dwc.locality?:"",
                        dwc.multimedia ? dwc.multimedia.collect {it.identifier}.join(";") : "",
                        dwc.modified?:"",
                        dwc.locationID?:""
                ] as String[]
        )
    }
}
