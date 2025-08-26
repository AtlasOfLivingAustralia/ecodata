package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.databinding.BindingFormat

/**
 * Stores details of a target a project plans to achieve.
 */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class OutputTarget {

    static constraints = {
        targetDate nullable: true
    }

    static embedded = ['periodTargets', 'outcomeTargets']

    /** The scoreId of the Score entity used to measure progress towards this OutputTarget */
    String scoreId

    /** The target the project wishes to achieve as measured by the Score identified by scoreId */
    BigDecimal target

    /** List of milestone targets related to the same Score as this OutputTarget */
    List<PeriodTarget> periodTargets

    /** Breakdown of the overall target by project outcome addressed by this target measure */
    List<OutcomeTarget> outcomeTargets

    /** Optional date this target will be achieved by (the default is the end of the project) */
    @BindingFormat('iso8601')
    Date targetDate

}
