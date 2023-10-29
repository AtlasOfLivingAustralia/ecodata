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

    /** Custom toMap for the bulk import object */
    Map toMap() {
        Map bulkImport = [:]
        bulkImport.bulkImportId = bulkImportId
        bulkImport.dataToLoad = dataToLoad
        bulkImport.projectActivityId = projectActivityId
        bulkImport.projectId = projectId
        bulkImport.formName = formName
        bulkImport.description = description
        bulkImport.createdActivities = createdActivities
        bulkImport.validActivities = validActivities
        bulkImport.invalidActivities = invalidActivities
        bulkImport.userId = userId
        bulkImport.status = status
        bulkImport.dateCreated = dateCreated
        bulkImport.lastUpdated = lastUpdated

        bulkImport
    }

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
