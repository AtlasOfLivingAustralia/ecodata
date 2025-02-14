package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.EqualsAndHashCode

/**
 * Container for geographic information about a project that is not derived from spatial data. (e.g. Sites)
 */
@EqualsAndHashCode
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class GeographicInfo {

    static constraints = {
        primaryState nullable: true
        primaryElectorate nullable: true
        otherStates nullable: true
        otherElectorates nullable: true
        otherExcludedStates nullable: true
        otherExcludedElectorates nullable: true
    }

    /** Some projects don't have specific geographic areas and are flagged as being run nationwide */
    boolean nationwide = false

    /** A flag to indicate that the project is running statewide i.e. all electorates in a state */
    boolean statewide = false

    /** A flag to override calculated primary state value */
    boolean overridePrimaryState = false

    /** A flag to override calculated primary electorate value */
    boolean overridePrimaryElectorate = false

    /** The primary state in which this project is running, if applicable */
    String primaryState

    /** The primary electorate in which this project is running, if applicable */
    String primaryElectorate

    /** States in which this project is running */
    List<String> otherStates

    /** States to exclude from project list */
    List<String> otherExcludedStates

    /** Electorates in which this project is running */
    List<String> otherElectorates

    /** Electorates to exclude from project list */
    List<String> otherExcludedElectorates
}
