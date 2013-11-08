package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * Represents documents stored in a filesystem that are accessible via http.
 */
class Document {

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
    }
}
