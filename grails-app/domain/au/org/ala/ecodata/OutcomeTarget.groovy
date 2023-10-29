package au.org.ala.ecodata

/**
 * A milestone target - used to track per financial year minimum targets for some programs.
 */
class OutcomeTarget {

    static constraints = {
    }

    /** A label that describes the period or milestone date this target is relevant to */
    List<String> relatedOutcomes

    /** The target to be achieved during the period */
    BigDecimal target
}
