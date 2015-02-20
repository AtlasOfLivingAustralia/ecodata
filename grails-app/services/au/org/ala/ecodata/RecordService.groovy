package au.org.ala.ecodata

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

class RecordService {

    def grailsApplication

    def serviceMethod() {}

    final def ignores = ["action","controller","associatedMedia"]

    def broadcastService

    def userFielddataService

    def createRecord(json){
        Record record = new Record().save(true)
        def errors = updateRecord(record,json)
        [record, errors]
    }

    private def updateRecord(Record record,  json){
        def errors =[:]
        try {
            json.each {
                if(!ignores.contains(it.key) && it.value){
                    record[it.key] = it.value
                }
            }

            //persist an images into image service
            if(json.multimedia){
                json.multimedia.eachWithIndex { image, idx ->
                    def address = image.identifier ? image.identifier : image.url
                    def downloadedFile = download(record.occurrenceID, idx, address)
                    def imageId = uploadImage(record, downloadedFile)
                    record.multimedia[idx].imageId = imageId
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


    private def uploadImage (Record record, File fileToUpload) {

        def remoteImageRepo = "http://images-dev.ala.org.au/"
        //upload an image
        def httpClient = new DefaultHttpClient()
        def entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
        if (!fileToUpload.exists()) {
            log.error("File to upload does not exist or can not be read. " + fileToUpload.getAbsolutePath())
        } else {
            log.debug("File to upload: " + fileToUpload.getAbsolutePath() + ", size:" + fileToUpload.length())
        }
        def fileBody = new FileBody(fileToUpload, "image/jpeg")

        entity.addPart("image", fileBody)
        entity.addPart("metadata", new StringBody((["occurrenceId": record.occurrenceID] as JSON).toString()))


        def httpPost = new HttpPost(remoteImageRepo + "/ws/uploadImage")
        httpPost.setEntity(entity)
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
        //add userDisplayName - Cache-able not working....
        if(mapOfProperties["userId"]){
            def userMap = userFielddataService.getUserNamesForIdsMap()
            def userId = mapOfProperties["userId"]
            def userDisplayName = userMap.get(userId)
            if(userDisplayName){
                 mapOfProperties["userDisplayName"] = userDisplayName
            }
        }
        mapOfProperties
    }
}