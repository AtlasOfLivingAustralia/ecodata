package au.org.ala.ecodata

import grails.util.Holders
import org.bson.types.ObjectId
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes

import static au.org.ala.ecodata.Status.ACTIVE
/**
 * Represents documents stored in a filesystem that are accessible via http.
 */
class Document {

    static final String ROLE_BANNER = 'banner'
    static final String ROLE_FOOTER_LOGO = 'footerlogo'
    static final String ROLE_LOGO = 'logo'
    static final String ROLE_HELP_RESOURCE = 'helpResource'
    static final String ROLE_MAIN_IMAGE = 'mainImage'

    /** If a document is one of these roles, it is implicitly public */
    static final List PUBLIC_ROLES = [ROLE_BANNER, ROLE_LOGO, ROLE_HELP_RESOURCE, ROLE_FOOTER_LOGO, ROLE_MAIN_IMAGE]

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
    String citation
    String doiLink

    String status = ACTIVE
    String projectId
    String siteId
    String activityId
    String projectActivityId
    String outputId
    String organisationId
    String programId
    String reportId
    String managementUnitId
    String hubId
    String externalUrl
    Boolean isSciStarter = false
    String hosted
    String identifier
    /* To be replaced by reportId */
    String stage

    boolean thirdPartyConsentDeclarationMade = false
    String thirdPartyConsentDeclarationText

    Date dateCreated
    Date lastUpdated

    // https://github.com/AtlasOfLivingAustralia/ecodata/issues/565
    // Only relevant for image document
    Date dateTaken
	boolean isPrimaryProjectImage = false

    /** The content type of the file related to this document */
    String contentType

    def isImage() {
        return DOCUMENT_TYPE_IMAGE == type
    }

    boolean isPubliclyViewable() {
        this['public'] || role in PUBLIC_ROLES || (hosted == ALA_IMAGE_SERVER)
    }

    def getUrl() {
        if (externalUrl) return externalUrl

        return urlFor(filepath, filename)
    }

    def getThumbnailUrl() {
        if (isImage()) {

            if(hosted == ALA_IMAGE_SERVER){
                return identifier
            }

            File thumbFile = new File(filePath(THUMBNAIL_PREFIX+filename))
            if (thumbFile.exists()) {
                return urlFor(filepath, THUMBNAIL_PREFIX + filename)
            }
            else {
                return getUrl()
            }
        }
        return ''
    }

    /**
     * Returns a String containing the URL by which the file attached to the supplied document can be downloaded.
     */
    private String urlFor(path, name) {
        if (!name) {
            return ''
        }

        if(hosted == ALA_IMAGE_SERVER){
            return identifier
        }

        String hostName = GrailsWebRequest.lookup()?.getAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, RequestAttributes.SCOPE_REQUEST) ?: ""
        path = path?path+'/':''

        def encodedFileName = URLEncoder.encode(name, 'UTF-8').replaceAll('\\+', '%20')
        URI uri = new URI(hostName + Holders.config.getProperty('app.uploads.url') + path + encodedFileName)
        return uri.toString()
    }

    private String filePath(name) {

        def path = filepath ?: ''
        if (path) {
            path = path+File.separator
        }
        return Holders.config.getProperty('app.file.upload.path') + '/' + path  + name

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
        reportId nullable: true
        managementUnitId nullable: true
        stage nullable: true
        filename nullable: true
        dateCreated nullable: true
        lastUpdated nullable: true
        dateTaken nullable: true
		isPrimaryProjectImage nullable: true
        thirdPartyConsentDeclarationMade nullable: true
        thirdPartyConsentDeclarationText nullable: true
        externalUrl nullable: true
        projectActivityId nullable: true
        labels nullable: true
        citation nullable: true
        doiLink nullable: true
        isSciStarter nullable: true
        hosted nullable: true
        identifier nullable: true
        contentType nullable: true
        hubId nullable: true
    }
}
