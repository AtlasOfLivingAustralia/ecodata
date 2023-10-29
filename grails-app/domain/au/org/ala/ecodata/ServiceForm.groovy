package au.org.ala.ecodata

/**
 * Configures the relationship between a Service and a ActivityForm type that can be
 * used to record data about that service.
 */
class ServiceForm {

    /** This isn't a reference to an activity form as the service can be represented by multiple versions of a form / section */
    String formName
    String sectionName
    /** Paratoo protocols are defined by id rather than a name */
    Integer externalId

    /** The list of scores that can be derived from the form */
    List<Score> relatedScores

    static constraints = {
        sectionName nullable: true
        externalId nullable: true
    }

}
