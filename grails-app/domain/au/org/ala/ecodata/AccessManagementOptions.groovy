package au.org.ala.ecodata

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.time.Period

/** This is a configuration class that manages the settings for when to expire user access for this hub */
@Slf4j
@CompileStatic
class AccessManagementOptions {

    /** Must be a string parsable by @see java.time.Period.parse */
    String expireUsersAfterPeriodInactive = "P24M"

    /** Must be a string parsable by @see java.time.Period.parse */
    String warnUsersAfterPeriodInactive = "P23M"

    Period getAccessExpiryPeriod() {
        Period result = null
        try {
            result = Period.parse(expireUsersAfterPeriodInactive)
        }
        catch (Exception e) {
            log.error("Invalid duration specified "+expireUsersAfterPeriodInactive)
        }
        result
    }

    Period getAccessExpiryWarningPeriod() {
        Period result = null
        try {
            result = Period.parse(warnUsersAfterPeriodInactive)
        }
        catch (Exception e) {
            log.error("Invalid warning duration specified "+warnUsersAfterPeriodInactive)
        }
        result
    }
}
