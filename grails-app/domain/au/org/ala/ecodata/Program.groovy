package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A program acts as a container for projects, more or less.
 */
class Program {

    ObjectId id
    String programId
    String name
    String description
    String status = 'active'
    Date dateCreated
    Date lastUpdated
    List blog
    List risks

    Date startDate
    Date endDate

    List<Program> subPrograms

    static mapping = {
        programId index: true
        version false
    }

    static constraints = {
        name unique: true
        description nullable: true
        blog nullable: true
        risks nullable: true
        subPrograms nullable: true
        startDate nullable: true
        endDate nullable: true
    }
}
