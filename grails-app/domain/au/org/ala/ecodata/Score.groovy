package au.org.ala.ecodata

class Score {

    Long id

    String status = 'active'
    
    String scoreId

    /** The label for this score when displayed */
    String label

    /** A more detailed description of the score and how it should be interpreted */
    String description

    /** Whether or not this score is suitable for use as a project output target */
    boolean isOutputTarget

    String outputType

    String category

    String displayType

    /** The entity this score is derived from (Outputs are treated as a part of an Activity for the purposes of scoring */
    String entity

    /** Allows this score to be identified by an external system (e.g. GMS for loading output targets) */
    String externalId

    /** In the case that the score is derived from an Activity, this contains the activity types used */
    List<String> entityTypes


    /** Embedded document describing how the score should be calculated */
    Map configuration

    static constraints = {
        outputType nullable:true
        category nullable:true
        displayType nullable:true
        description nullable:true
        entityTypes nullable:true
        externalId nullable:true
        label unique: true
        scoreId unique: true
    }

    static mapping = {

        scoreId index: true
        version false
    }

    def beforeValidate() {
        if (scoreId == null) {
            scoreId = Identifiers.getNew(true, "")
        }
    }
}
