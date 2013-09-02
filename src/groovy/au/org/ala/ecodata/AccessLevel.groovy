package au.org.ala.ecodata

/**
 * Enum for access levels
 *
 * Note: "starred" might need to be moved as its not a perfect fit here
 * "validator" is not used so could be removed if not required.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public enum AccessLevel {
    admin, validator, editor, starred
}
