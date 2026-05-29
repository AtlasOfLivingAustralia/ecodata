package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.databinding.BindingFormat

/**
 * A milestone target - used to track per financial year minimum or forecast targets for some programs.
 */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class PeriodTarget {

    static constraints = {
        periodStart nullable:true
        periodEnd nullable:true
    }

    /** A label that describes the period or milestone date this target is relevant to */
    String period

    /** The target to be achieved during the period */
    BigDecimal target

    @BindingFormat('iso8601')
    Date periodStart

    @BindingFormat('iso8601')
    Date periodEnd
}
