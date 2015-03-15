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
                "systemSupplier": grailsApplication.config.imageSysteSupplier?:"ecodata"
        ] as JSON).toString()))

        if (record.tags?.size() > 0) {
            entity.addPart("tags", record.tags.join(","))
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
}
