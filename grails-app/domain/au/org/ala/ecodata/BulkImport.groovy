package au.org.ala.ecodata

import org.bson.types.ObjectId

class BulkImport {
    ObjectId id

    String bulkImportId
    List dataToLoad
    String projectActivityId
    String projectId
    String formName
    String description
    List createdActivities
    List validActivities
    List invalidActivities
    String userId

    String status = 'active'
    Date dateCreated
    Date lastUpdated

    static constraints = {
        createdActivities nullable: true
        validActivities nullable: true
        invalidActivities nullable: true
    }

    static mapping = {
        bulkImportId index: true
        projectActivityId index: true
        projectId index: true
        userId index: true
    }
}
