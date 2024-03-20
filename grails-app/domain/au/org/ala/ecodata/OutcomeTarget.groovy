package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * A milestone target - used to track per financial year minimum targets for some programs.
 */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class OutcomeTarget {

    static constraints = {
    }

    /** A label that describes the period or milestone date this target is relevant to */
    List<String> relatedOutcomes

    /** The target to be achieved during the period */
    BigDecimal target
}
