package au.org.ala.ecodata

import grails.core.support.GrailsConfigurationAware

//import grails.core.GrailsApplication
import grails.config.Config
import org.springframework.beans.factory.annotation.Value;

import static au.org.ala.ecodata.Status.ACTIVE

import org.bson.types.ObjectId

/**
 * Represents documents stored in a filesystem that are accessible via http.
 */
class Document {//implements GrailsConfigurationAware{


   // def grailsApplication

    static final String DOCUMENT_TYPE_IMAGE = 'image'
    static final String THUMBNAIL_PREFIX = 'thumb_'
    static final String PROCESSED_PREFIX = 'processed_'
    static final String ALA_IMAGE_SERVER = 'images.ala.org.au'

    static mapping = {
        projectId index: true
        siteId index: true
        activityId index: true
        projectActivityId index: true
        outputId index: true
        organisationId index: true
        programId index: true
        status index: true
        role index: true
        version false
   //     autowire true
    }

    ObjectId id
    String documentId
    String name // caption, document title, etc
    String attribution  // source, owner
    String licence
    String filename
    String filepath
    String type // image, document, sound, etc
    String role // eg primary, carousel, photoPoint
    List<String> labels = [] // allow for searching on custom attributes

    String status = ACTIVE
    String projectId
    String siteId
    String activityId
    String projectActivityId
    String outputId
    String organisationId
    String programId
    String externalUrl
    Boolean isSciStarter = false
    String hosted
    String identifier
    /** The original content type of the uploaded document */
    String contentType
    /** Probably should be implemented as a label - used to tag this document to a report in MERIT*/
    String stage
    /** the size of the attachment associated with this document */
    Long filesize
    /** Flag to indicate that this document shouldn't be updated once created */
    boolean readOnly = false

    boolean thirdPartyConsentDeclarationMade = false
    String thirdPartyConsentDeclarationText

    Date dateCreated
    Date lastUpdated
	boolean isPrimaryProjectImage = false

   // @Value('${app.file.upload.path}')
  //  String fileUploadPath

   // @Value('${app.uploads.url}')
   // String fileUploadUrl

   // static transients = ['fileUploadPath', 'fileUploadUrl']

    def isImage() {
        return DOCUMENT_TYPE_IMAGE == type
    }

  /*  @Override
    void setConfiguration(Config config) {
        fileUploadPath = config.getProperty('app.file.upload.path')
        fileUploadUrl = config.getProperty('app.uploads.url')
    }*/

    def getUrl(uploadUrl) {
        if (externalUrl) return externalUrl

        return urlFor(uploadUrl, filepath, filename)
    }

    def getThumbnailUrl(uploadPath, uploadUrl) {
        if (isImage()) {

            if(hosted == ALA_IMAGE_SERVER){
                return identifier
            }

            File thumbFile = new File(filePath(uploadPath, THUMBNAIL_PREFIX+filename))
            if (thumbFile.exists()) {
                return urlFor(uploadUrl, filepath, THUMBNAIL_PREFIX + filename)
            }
            else {
                return getUrl(uploadUrl)
            }
        }
        return ''
    }

    /**
     * Returns a String containing the URL by which the file attached to the supplied document can be downloaded.
     */
    private def urlFor(uploadUrl, path, name) {
        if (!name) {
            return ''
        }

        if(hosted == ALA_IMAGE_SERVER){
            return identifier
        }

        path = path?path+'/':''

        def encodedFileName = URLEncoder.encode(name, 'UTF-8').replaceAll('\\+', '%20')
        // URI uri = new URI(fileUploadUrl + path + encodedFileName)
        // URI uri = new URI(grailsApplication.config.getProperty('app.uploads.url') + path + encodedFileName)
        URI uri = new URI(uploadUrl + path + encodedFileName)
        return uri.toURL();
    }

    private def filePath(fileUploadPath, name) {

        def path = filepath ?: ''
        if (path) {
            path = path+File.separator
        }
       // return grailsApplication.config.app.file.upload.path + '/' + path  + name
        return fileUploadPath + '/' + path  + name

    }

    static constraints = {
        name nullable: true
        attribution nullable: true
        licence nullable: true
        filepath nullable: true
        type nullable: true
        role nullable: true
        projectId nullable: true
        organisationId nullable: true
        siteId nullable: true
        activityId nullable: true
        outputId nullable: true
        programId nullable: true
        filename nullable: true
        dateCreated nullable: true
        lastUpdated nullable: true
		isPrimaryProjectImage nullable: true
        thirdPartyConsentDeclarationMade nullable: true
        thirdPartyConsentDeclarationText nullable: true
        externalUrl nullable: true
        projectActivityId nullable: true
        labels nullable: true
        isSciStarter nullable: true
        hosted nullable: true
        identifier nullable: true
        contentType nullable: true
        stage nullable: true
        filesize nullable: true
    }
}
