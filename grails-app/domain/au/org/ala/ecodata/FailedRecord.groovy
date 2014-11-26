package au.org.ala.ecodata

/**
 * Represents a records that has failed to be submitted to the broadcast service.
 *
 * These will need to reschedule at a later time.
 *
 */
import org.bson.types.ObjectId
class FailedRecord {

    static mapping = { version false }
    ObjectId id
    Date lastAttempted
    Integer numberOfAttempts
    String updateType

    static belongsTo = [record:Record]

    static constraints = {
    }
}
