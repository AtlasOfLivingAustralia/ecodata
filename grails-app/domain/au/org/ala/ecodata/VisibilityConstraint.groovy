package au.org.ala.ecodata

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Created by cha693 on 27/10/15.
 */
@ToString
@EqualsAndHashCode
class VisibilityConstraint {
    EmbargoOption embargoOption = EmbargoOption.NONE
    Integer embargoForDays
    Date embargoUntil // either the user-entered date when EmbargoOption.DATE is used, OR the calculated date when EmbargoOption.DAYS is used

    static constraints = {
        embargoForDays nullable: true
        embargoOption nullable: true
        embargoUntil nullable: true
    }
}
