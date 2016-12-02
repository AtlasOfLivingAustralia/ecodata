package au.org.ala.ecodata

class Score {

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

    /** Embedded document describing how the score should be calculated */
    Map configuration

    static constraints = {
        outputType nullable:true
        category nullable:true
        displayType nullable:true
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
