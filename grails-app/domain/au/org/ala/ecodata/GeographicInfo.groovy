package au.org.ala.ecodata

import groovy.transform.EqualsAndHashCode

/**
 * Container for geographic information about a project that is not derived from spatial data. (e.g. Sites)
 */
@EqualsAndHashCode
class GeographicInfo {

    static constraints = {
        primaryState nullable: true
        primaryElectorate nullable: true
        otherStates nullable: true
        otherElectorates nullable: true
    }

    /** Some projects don't have specific geographic areas and are flagged as being run nationwide */
    boolean nationwide = false

    /** The primary state in which this project is running, if applicable */
    String primaryState

    /** The primary electorate in which this project is running, if applicable */
    String primaryElectorate

    /** States in which this project is running */
    List<String> otherStates

    /** Electorates in which this project is running */
    List<String> otherElectorates
}
