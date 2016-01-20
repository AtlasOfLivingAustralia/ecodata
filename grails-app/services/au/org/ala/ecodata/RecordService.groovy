package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

import au.com.bytecode.opencsv.CSVWriter
import au.org.ala.web.AuthService
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
    ActivityService activityService
    MetadataService metadataService
    OutputService outputService
    ProjectService projectService
    SiteService siteService
    AuthService authService
    UserService userService

    final def ignores = ["action", "controller", "associatedMedia"]
    private static final List<String> EXCLUDED_RECORD_PROPERTIES = ["_id", "activityId", "dateCreated", "json", "outputId", "projectActivityId", "projectId", "status", "dataResourceUid"]

    def exportRecordsToCSV(OutputStream outputStream, String projectId, String userId, List<String> restrictedProjectActivities) {
        // Different Records may have different DwC attributes, as these are based on the 'dwcAttribute' mapping in the
        // Output Metadata, so first we need to determine the full set of unique property names for all records...
        Set<String> properties = []

        def attributeCollection = Record.collection.mapReduce("function map() {" +
                "    for (var key in this) { emit(key, null); }" +
                "  }",
                "function reduce(key, stuff) { return null; }",
                "attributeCollection", [:])

        properties.addAll(attributeCollection.results().findAll().collect { it._id })
        attributeCollection.drop()

        // ...then we can exclude any properties that we do not want in the CSV
        properties.removeAll(EXCLUDED_RECORD_PROPERTIES)

        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))
        csvWriter.writeNext(properties as String[])

        List<Record> recordList = Record.withCriteria {
            if (projectId) {
                eq "projectId", projectId
            }
            ne "status", DELETED

            // exclude records that do not have lat/lng coords
            isNotNull "decimalLatitude"
            isNotNull "decimalLongitude"

            or {
                eq "userId", userId
                not { 'in' "projectActivityId", restrictedProjectActivities }
            }
        }

        log.info("Number of records to export: ${recordList.size()}")

        // write out each record
        recordList.each {
            Map map = toMap(it)
            String[] row = properties.collect {
                if (it == "multimedia") {
                    map.multimedia?.collect { it.identifier }?.join(";")
                } else if (it == "lastUpdated") {
                    map.lastUpdated?.format("dd-MM-yyyy")
                } else {
                    map[it]
                }
            }
            csvWriter.writeNext(row)
        }

        csvWriter.flush()
        csvWriter.close()
    }

    /**
     * Export records to CSV for a project. This implementation is unlikely to scale beyond 50k
     * records.
     */
    private exportRecordBasedProject(CSVWriter csvWriter, String projectId, String userId, List<String> restrictedProjectActivities) {
        List<Record> recordList = Record.withCriteria {
            eq "projectId", projectId
            ne "status", DELETED

            or {
                eq "userId", userId
                not { 'in' "projectActivityId", restrictedProjectActivities }
            }
        }

        log.info("Number of records to export: ${recordList.size()}")

        //write out each record
        recordList.each {
            Map map = toMap(it)
            csvWriter.writeNext([
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
            ] as String[])
        }
        csvWriter.flush()
    }

    /**
     * Create a record based on the supplied JSON
     *
     * @param json
     * @return
     */
    def createRecord(json){
        Record record = new Record().save(true)
        def errors = updateRecord(record, json)
        [record, errors]
    }

    def getAllByActivity(String activityId) {
        Record.findAllByActivityIdAndStatus(activityId, ACTIVE).collect { toMap(it) }
    }

    def getAllByProject(String projectId) {
        Record.findAllByProjectIdAndStatus(projectId, ACTIVE).collect { toMap(it) }
    }

    /**
     * Create a record with the supplied map of fileName -> byte[].
     *
     * @param json
     * @param fileMap
     */
    def createRecordWithImages(json, fileMap){
        Record record = new Record().save(true)
        def errors = updateRecord(record, json, fileMap)
        [record, errors]
    }

    /**
     * Update the supplied record including updates to any supplied images.
     * This method will take imageMetadata references in the json in the multimedia section
     * or will upload images supplied in the imageMap which has contains
     * filename -> byte[]
     *
     * @param record The Record to be updated
     * @param json A map or JSONObject containing the record data. This data must contain a userId.
     * @param imageMap a map of image resources to be associated with the record
     *
     * @return Map of errors, or an empty map if there were no errors.
     */
    private Map updateRecord(Record record, json, Map imageMap = [:]) {
        Map errors = [:]

        try {
            def userDetails = userService.getCurrentUserDetails()
            if (!userDetails && json.userId) {
                userDetails = authService.getUserForUserId(json.userId)
            }

            if (!userDetails) {
                errors['updateError'] = "Unable to lookup user with ID: ${json.userId}. Check authorised systems in auth."
                return errors
            }
            record.userId = userDetails.userId
            record.recordedBy = userDetails.displayName

            //set all supplied properties
            json.each {
                if (it.key in ["decimalLatitude", "decimalLongitude"] && it.value) {
                    record[it.key] = it.value.toString().toDouble()
                } else if (it.key in ["coordinateUncertaintyInMeters", "individualCount"] && it.value) {
                    record[it.key] = it.value.toString().toInteger()
                } else if (it.key in ["dateCreated", "lastUpdated"] && it.value) {
                    //do nothing we these values...
                } else if (!ignores.contains(it.key) && it.value) {
                    record[it.key] = it.value
                }
            }

            //if no projectId is supplied, use default
            if (!record.projectId) {
                record.projectId = grailsApplication.config.records.default.projectId
            }

            //use the data resource UID associated with the project
            def project = Project.findByProjectId(record.projectId)
            record.dataResourceUid = project.dataResourceId

            //clear current imageMetadata references on the record
            record.multimedia = []

            //persist any supplied images into imageMetadata service
            if (json.multimedia) {

                json.multimedia.eachWithIndex { image, idx ->

                    record.multimedia[idx] = [:]

                    // Each image in Ecodata may have an associated Document entity. We need to maintain this relationship in the resulting Record entity
                    record.multimedia[idx].documentId = image.documentId

                    // reconcile new with old images...
                    // Only upload images that are NOT already in images.ala.org.au
                    if (!image.creator) {
                        image.creator = userDetails.displayName
                    }

                    if (!image.rightsHolder) {
                        image.rightsHolder = userDetails.displayName
                    }

                    def alreadyLoaded = false
                    if (!image.imageId) {
                        log.debug "Uploading imageMetadata - ${image.identifier}"
                        def downloadedFile = download(record.occurrenceID, idx, image.identifier)
                        def imageId = uploadImage(record, downloadedFile, image)

                        record.multimedia[idx].imageId = imageId
                        record.multimedia[idx].identifier = getImageUrl(imageId)

                    } else {
                        alreadyLoaded = true
                        //re-use the existing imageId rather than upload again
                        log.debug "Image already uploaded - ${image.imageId}"
                        record.multimedia[idx].imageId = image.imageId
                        record.multimedia[idx].identifier = image.identifier
                    }

                    setDCTerms(image, record.multimedia[idx])

                    if (alreadyLoaded) {
                        log.debug "Refreshing metadata - ${image.identifier}"
                        //refresh metadata in imageMetadata service
                        updateImageMetadata(image.imageId, record, record.multimedia[idx])
                    }
                }
            } else if (imageMap) {
                //upload the images supplied as bytes
                def idx = 0
                imageMap.each { imageFileName, imageInBytes ->
                    def metadata = [
                            title  : imageFileName,
                            creator: userDetails.displayName
                    ]
                    def imageId = uploadImageInByteArray(record, imageFileName, imageInBytes, metadata)
                    record.multimedia[idx] = [:]
                    record.multimedia[idx].imageId = imageId
                    record.multimedia[idx].identifier = getImageUrl(imageId)
                    //we only support one set of metadata for all images for this method
                    setDCTerms(json, record.multimedia[idx])
                    idx++
                }
            }

            record.save(flush: true)
        } catch (Exception e) {
            log.error(e.getMessage(), e)
            errors['updateError'] = e.getClass().toString() + " " + e.getMessage()
        }

        errors
    }

    private setDCTerms(image, multimediaElement){
        multimediaElement.license = image.license
        multimediaElement.rights = image.rights
        multimediaElement.rightsHolder = image.rightsHolder
        multimediaElement.title = image.title
        multimediaElement.type = image.type
        multimediaElement.format = image.format
        multimediaElement.creator = image.creator
    }

    private def getImageUrl(imageId){
        grailsApplication.config.imagesService.baseURL + "/image/proxyImageThumbnailLarge?imageId=" + imageId
    }

    /**
     * Add the supplied imageMetadata in bytes to the supplied record.
     *
     * @param record
     * @param originalName
     * @param imageAsBytes
     */
    private def uploadImageInByteArray(Record record, String originalName, byte[] imageAsBytes, Map metadata){

        //write bytes to temp file
        def fileToUpload = File.createTempFile("multipart-upload-" + System.currentTimeMillis(),".tmp")
        fileToUpload.withOutputStream {
            it.write imageAsBytes
        }

        //upload
        def imageId = uploadImage(record, fileToUpload, metadata)

        if(!record.multimedia){
            record.multimedia = []
        }

        //remove temp file
        fileToUpload.delete()

        //return imageMetadata Id
        imageId
    }

    /**
     * Update the metadata for an imageMetadata in the imageMetadata service.
     * This will include updates to tags and licensing.
     *
     * @param imageId
     * @param record
     * @param metadataProperties
     * @return
     */
    private def updateImageMetadata(String imageId, record, metadataProperties){

        log.info("Updating imageMetadata metadata for imageMetadata: ${imageId} from record ${record.occurrenceID}")

        //upload an image metadata
        def entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
        entity.addPart("metadata", new StringBody(([
                "title": metadataProperties.title,
                "creator": metadataProperties.creator,
                "rights": metadataProperties.rights,
                "rightsHolder": metadataProperties.rightsHolder ? metadataProperties.rightsHolder : metadataProperties.creator,
                "license": metadataProperties.license
        ] as JSON).toString()))

        if (record.tags) {
            entity.addPart("tags", new StringBody((record.tags as JSON).toString()))
        }

        def httpClient = new DefaultHttpClient()
        def httpPost = new HttpPost(grailsApplication.config.imagesService.baseURL + "/ws/updateMetadata/${imageId}")
        httpPost.setEntity(entity)
        httpPost.addHeader("X-ALA-userId", "${record.userId}");
        def response = httpClient.execute(httpPost)
        def result = response.getStatusLine()
    }

    /**
     * Upload the supplied imageMetadata to the imageMetadata service.
     *
     * @param record
     * @param fileToUpload
     * @param imageMetadata
     * @return
     */
    private def uploadImage (Record record, File fileToUpload, imageMetadata) {

        //upload an imageMetadata
        def entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
        if (!fileToUpload.exists()) {
            log.error("File to upload does not exist or can not be read. " + fileToUpload.getAbsolutePath())
        } else {
            log.debug("File to upload: " + fileToUpload.getAbsolutePath() + ", size:" + fileToUpload.length())
        }
        def fileBody = new FileBody(fileToUpload, "image/jpeg")
        log.debug "imageMetadata upload: ${imageMetadata}"
        entity.addPart("image", fileBody)
        entity.addPart("metadata", new StringBody(([
                "occurrenceId": record.occurrenceID,
                "projectId": record.projectId,
                "dataResourceUid": record.dataResourceUid,
                "originalFilename": imageMetadata.title,
                "title": imageMetadata.title,
                "creator": imageMetadata.creator,
                "rights": imageMetadata.rights,
                "rightsHolder": imageMetadata.rightsHolder ? imageMetadata.rightsHolder : imageMetadata.creator,
                "license": imageMetadata.license,
                "dateTaken": imageMetadata?.created,
                "systemSupplier": grailsApplication.config.imageSystemSupplier?:"ecodata"
        ] as JSON).toString()))

        if (record.tags) {
            entity.addPart("tags", new StringBody((record.tags as JSON).toString()))
        }

        def httpClient = new DefaultHttpClient()
        def httpPost = new HttpPost(grailsApplication.config.imagesService.baseURL + "/ws/uploadImage")
        httpPost.setEntity(entity)
        httpPost.addHeader("X-ALA-userId", "${record.userId}");
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
    def exportCSVProject(OutputStream outputStream, String projectId, String modelName, String userId, List<String> restrictedProjectActivities){
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))
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

        List<Map> projects
        if (projectId) {
            projects = [projectService.get(projectId, projectService.FLAT)]
        } else {
            projects = projectService.search([:], projectService.FLAT)
        }

        projects.each { Map project ->
            exportActivityBasedProject(csvWriter, project, modelName)
            exportRecordBasedProject(csvWriter, project.projectId, userId, restrictedProjectActivities)
        }

        csvWriter.flush()
        csvWriter.close()
    }

    /**
     * Export activities to CSV of darwin core terms.
     *
     * @param csvWriter
     * @param project
     * @param modelName
     */
    private def exportActivityBasedProject(csvWriter, project, modelName) {

        def modelsMap = [:]
        def projectSite = siteService.get(project.projectSiteId)
        def today = (new Date()).format("dd-MM-yyyy")
        def projectDates = mapDates(today, project.plannedStartDate, project.plannedEndDate, project.actualStartDate, project.actualEndDate)

        //export activity based data
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

    /**
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
