package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * Represents documents stored in a filesystem that are accessible via http.
 */
class Document {

    def grailsApplication

    static final String DOCUMENT_TYPE_IMAGE = 'image'
    static final String THUMBNAIL_PREFIX = 'thumb_'

    static mapping = {
        name index: true
        projectId index: true
        siteId index: true
        activityId index: true
        outputId index: true
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

    String status = 'active'
    String projectId
    String siteId
    String activityId
    String outputId

    Date dateCreated
    Date lastUpdated

    def isImage() {
        return DOCUMENT_TYPE_IMAGE == type
    }

    def getUrl() {
        return urlFor(filepath, filename)
    }

    def getThumbnailUrl() {
        if (isImage()) {

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
    private def urlFor(path, name) {
        if (!name) {
            return ''
        }

        path = path?path+'/':''

        def encodedFileName = name.encodeAsURL().replaceAll('\\+', '%20')
        URI uri = new URI(grailsApplication.config.app.uploads.url + path + encodedFileName)
        return uri.toURL();
    }

    private def filePath(name) {

        def path = filepath ?: ''
        if (path) {
            path = path+File.separator
        }
        return grailsApplication.config.app.file.upload.path + '/' + path  + name

    }

    static constraints = {
        name nullable: true
        attribution nullable: true
        licence nullable: true
        filepath nullable: true
        type nullable: true
        role nullable: true
        projectId nullable: true
        siteId nullable: true
        activityId nullable: true
        outputId nullable: true
        filename nullable: true
        dateCreated nullable: true
        lastUpdated nullable: true
    }
}
