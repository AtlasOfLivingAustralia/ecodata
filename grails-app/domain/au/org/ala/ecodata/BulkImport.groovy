package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * BulkImport records all bulk upload of data via BioCollect. It is for ALA admins to be able to revisit bulk uploads.
 * It gives admins the ability to embargo, delete, publish or even upload a fresh set of data.
 */
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

    /** The list of properties to be used when binding request data to an ActivityForm */
    static bindingProperties = ['dataToLoad', 'projectActivityId', 'projectId', 'formName', 'description', 'createdActivities', 'validActivities', 'invalidActivities', 'userId']

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
