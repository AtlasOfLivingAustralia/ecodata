package au.org.ala.ecodata

import org.bson.types.ObjectId

class ProjectActivity {

    ObjectId id
    String projectActivityId
    String projectId
    String name
    String description
    String status
    String pActivityFormName
    boolean commentsAllowed
    Date startDate
    Date endDate
    Map alerts
    List sites = [] // list of sites associated to the survey
    static constraints = {
        pActivityFormName nullable : true
        alerts nullable:true
        sites nullable:true
    }
}
