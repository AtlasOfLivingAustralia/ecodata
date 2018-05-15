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
    String status = Status.ACTIVE
    String url
    Date dateCreated
    Date lastUpdated
    List blog
    List risks
    /** Themes for this program */
    List themes
    /** Assets managed by this program (e.g. threatened species, or ecological communities) */
    List assets
    /** Outcomes to be achieved by this program */
    List outcomes

    /** Priorities for program outcomes */
    List priorities

    /** Configuration related to the program */
    Map config

    Date startDate
    Date endDate

    List<Program> subPrograms = []
    Program parent

    static mapping = {
        programId index: true
        version false
    }

    static hasMany = [subPrograms:Program]

    static constraints = {
        name unique: true
        description nullable: true
        blog nullable: true
        risks nullable: true
        subPrograms nullable: true
        startDate nullable: true
        endDate nullable: true
        url nullable: true
        config nullable: true
        parent nullable: true
    }

    public String toString() {
        return "Name: "+name+ ", description: "+description
    }
}
