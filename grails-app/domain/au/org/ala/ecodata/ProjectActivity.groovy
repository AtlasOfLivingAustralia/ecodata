package au.org.ala.ecodata

import org.bson.types.ObjectId

class ProjectActivity {

    ObjectId id
    String projectActivityId
    String projectId
    String name
    String description
    String status
    boolean commentsAllowed
    Date startDate
    Date endDate

    static constraints = {
    }
}
