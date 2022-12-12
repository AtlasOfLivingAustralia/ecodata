package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A service that can be delivered by an organisation as a part of a project.
 * Details of the service delivered can be captured by various ActivityForms and measured by various Scores.
 * The Scores that can measured a service are determined by the types of ActivityForms used to record the
 * information about the service.
 */
class Service {

    ObjectId id
    /** This is an integer for legacy reasons - other references need to be migrated to the uuid before this is removed */
    int legacyId
    String serviceId
    String name
    List<String> categories
    List<ServiceForm> outputs
    String status = Status.ACTIVE

    static constraints = {
    }

    static embedded = ['outputs']
    List<Score> scores() {
        outputs?.collect{it.relatedScores}.unique().flatten()
    }

    Map toMap() {
        [
            id: legacyId,
            serviceId: serviceId,
            name: name,
            categories: categories,
            outputs: outputs?.collect {
                [
                        formName: it.formName,
                        sectionName: it.sectionName,
                        scoreIds: it.relatedScores.collect{it.scoreId}
                ]
            },
            scores: scores()
        ]
    }

}
