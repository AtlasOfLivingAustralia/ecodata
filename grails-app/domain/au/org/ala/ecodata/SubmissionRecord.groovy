package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A submission record keeps track of the submission details of the dataset that is submitted to AEKOS
 */
class SubmissionRecord {

    ObjectId id
    Date submissionPublicationDate
    String submissionDoi
    //String datasetSubmitter
    /** The user ID of the user who submitted this dataset */
    String datasetSubmitter
    String datasetVersion
    String submissionRecordId
    String submissionId
    String projectActivityId
    SubmissionPackage submissionPackage

    static constraints = {
        submissionPublicationDate   nullable:true
        submissionDoi               nullable:true
        datasetSubmitter            nullable:true
        datasetVersion              nullable:true
        submissionRecordId          nullable:true
        submissionPackage           nullable:true
        projectActivityId           nullable:true
        submissionId                nullable:true
    }

    static belongsTo = ProjectActivity

    static hasOne = [submissionPackage: SubmissionPackage]

   /*
    Association:
        submissionRecord must belong to a ProjectActivity
    */
    static mapping = {
        projectActivityId index: true
        submissionRecordId index: true
        submissionId  index: true
        version false
    }

}
