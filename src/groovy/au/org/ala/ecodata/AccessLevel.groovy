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
    admin(100), approver(60), editor(40), starred(20)

    private int code
    private AccessLevel(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
}
