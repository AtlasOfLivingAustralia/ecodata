package au.org.ala.ecodata

import org.springframework.web.multipart.MultipartFile
import java.text.SimpleDateFormat

import org.apache.commons.lang.time.DateUtils
import grails.converters.JSON
import org.apache.commons.codec.binary.Base64
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
 * Controller providing functions for mobile record submission from OzAtlas.
 */
class MobileController {

    def index() { }

    def recordService
    def authService
    def userService

    static def dateFormats = ["yyyy-MM-dd", "yyyy/MM/dd", "dd MMM yyyy", "dd-MM-yyyy", "dd/MM/yyyy", "dd/MM/yy"].toArray(new String[0])

    /**
     * Handles a multipart post with record details and images.
     */
    def submitRecordMultipart(){
        log.debug("Mobile - submitRecordMultipart POST received...userName:" + params.userName)
        def authenticated = checkUsername(params.userName)
        log.debug("Mobile userName:" + params.userName + ", recognised: " + authenticated)
        if (authenticated){
            try {
                log.debug("Mobile parsing parameters...")
                def recordParams = constructRecordParams(params)
                if (recordParams.record){
                    def (record,errors) = recordService.createRecord(recordParams.record)
                    if(errors.size() == 0){
                        log.debug("Mobile - record created: " + record?.id?.toString())
                        //handle the multipart message.....
                        if(request instanceof MultipartHttpServletRequest){
                            Map<String, MultipartFile> fileMap = request.getFileMap()
                            if (fileMap.containsKey("attribute_file_1")) {
                                MultipartFile multipartFile = fileMap.get("attribute_file_1")
                                byte[] imageAsBytes = multipartFile.getBytes()
                                String originalName = multipartFile.getOriginalFilename()
                                recordService.addImageToRecord(record, originalName, imageAsBytes)
                            }
                        }
                        log.debug "Added record: " + record.id.toString()
                        response.setStatus(200)
                        response.setContentType("application/json")
                        [success:true, recordId:record.id.toString()]
                    } else {
                        record.delete(flush: true)
                        response.setContentType("application/json")
                        log.error("Unable to create record. " + errors)
                        response.sendError(400, errors)
                        [success:false]
                    }
                } else {
                    response.setContentType("application/json")
                    response.sendError(400, recordParams.error)
                    [success:false]
                }
            } catch (Exception e){
                response.setStatus(500)
                response.setContentType("application/json")
                [success:false]
            }
        } else {
            response.setStatus(403)
            response.setContentType("application/json")
            [success:false]
        }
    }

    /**
     * Handles a record post with a single image encoded in base64.
     */
    def submitRecord(){
        log.debug("Mobile - submitRecord POST received...userName:" + params.userName)
        def authenticated = checkUsername(params.userName)
        log.debug("Mobile userName:" + params.userName + ", recognised: " + authenticated)
        if (authenticated){
            try {
                log.debug("Mobile parsing parameters...")
                def recordParams = constructRecordParams(params)
                if (recordParams.record){

                    def (record,errors) = recordService.createRecord(recordParams.record)
                    if(errors.size() == 0){
                        log.debug("Mobile - record created: " + record?.id?.toString())
                        //handle the base64 encoded image if supplied.....
                        if(params.imageBase64 && params.imageFileName){
                            byte[] imageAsBytes = Base64.decodeBase64(params.imageBase64)
                            recordService.addImageToRecord(record, params.imageFileName, imageAsBytes)
                        }
                        log.debug "submitRecord POST - added record: " + record.id.toString()
                        response.setStatus(200)
                        response.setContentType("application/json")
                        [success:true, recordId:record.id.toString()]
                    }
                    else{
                        record.delete(flush: true)
                        log.error("Unable to create record. " + errors)
                        response.sendError(400, errors)
                    }
                } else {
                    log.error("Unable to create record. " + recordParams.error)
                    response.sendError(400, recordParams.error)
                }
            } catch (Exception e){
                log.error(e.getMessage(),e)
                response.sendError(500, e.getMessage())
            }
        } else {
            log.info("AUTHENTICATION FAILED: Mobile userName:" + params.userName + ", authKey:" +params.authenticationKey)
            response.sendError(403, "Authentication failed")
        }
    }

    private def boolean checkUsername(String userName) throws Exception {
        //do we recognise the userName
        if(userName){
            userService.syncUserIdLookup(userName.toLowerCase()) != null
        } else {
            log.info("Supplied username is blank")
            false
        }
    }

    private def constructRecordParams(params){
        log.debug("Debug params....")
        params.each { log.debug("Received params: " + it) }
        def dateString = params.date
        def time = params.time
        def taxonId = params.taxonID
        def taxonName = params.survey_species_search
        def number = params.number
        def accuracyInMeters = params.accuracyInMeters
        def coordinatePrecision = params.coordinatePrecision
        def imageLicence = params.imageLicence
        log.debug("Multipart record request submission received.");

        // Convert date to desired format
        def dateToUse = null
        try {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = DateUtils.parseDate(dateString,dateFormats);
            dateToUse = dateFormatter.format(date)
        } catch (IllegalArgumentException ex) {
            log.debug("no date supplied: " + dateString)
            return [error:"no date supplied: " + dateString, record:null]
        } catch (Exception ex) {
            log.debug("invalid date format: " + dateString)
            return [error:"invalid date format: " + dateString, record:null]
        }

        //get the user Id....
        log.info("Retrieving...user ID with user name: " + params.userName)
        def userDetails = authService.getUserForEmailAddress(params.userName.toLowerCase())

        if(userDetails){
            log.debug("Retrieved user ID: " + userId+ ", for user name: " + params.userName)
            //save the files
            def recordParams = [
                   userId:userDetails.userId,
                   eventDate:dateToUse,
                   eventTime:time,
                   taxonConceptID:taxonId,
                   scientificName:taxonName,
                   family:params.family,
                   kingdom:params.kingdom,
                   decimalLongitude:params.longitude,
                   decimalLatitude:params.latitude,
                   individualCount:number,
                   coordinateUncertaintyInMeters:accuracyInMeters,
                   coordinatePrecision:coordinatePrecision,
                   imageLicence:imageLicence,
                   commonName:params.commonName,
                   locality:params.locationName,
                   device:params.deviceName,
                   devicePlatform:params.devicePlatform,
                   deviceId: params.deviceId,
                   occurrenceRemarks:params.notes,
                   submissionMethod: "mobile"
            ]
            log.debug((recordParams as JSON).toString(true))
            [error:null, record: recordParams]
        } else {
            log.error("Unable to retrieve a userId for username: " + params.userName)
            [error:"Authentication has failed for username: " + params.userName, record: null]
        }
    }
}
