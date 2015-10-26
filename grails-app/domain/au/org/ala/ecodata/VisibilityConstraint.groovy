package au.org.ala.ecodata

/**
 * Created by cha693 on 27/10/15.
 */
class VisibilityConstraint {
    EmbargoOption embargoOption = EmbargoOption.NONE
    Integer embargoForDays
    Date embargoUntil

    static constraints = {
        embargoForDays nullable: true
        embargoOption nullable: true
        embargoUntil nullable: true
    }
}
