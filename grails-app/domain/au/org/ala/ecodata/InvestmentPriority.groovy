package au.org.ala.ecodata

import org.bson.types.ObjectId
import org.springframework.validation.Errors

/**
 * An Investment Priority (also referred to as an Asset in older MERIT programs) is a Species/TEC/etc that
 * programs are funded to work on.  MERIT projects will have targeted investment priorities selected from
 * the set of investment priorities defined for the program.
 */
class InvestmentPriority {

    ObjectId id

    String investmentPriorityId
    String status = Status.ACTIVE

    /** Identifies the type of investment priority */
    String type

    /** Identifies the investment priority */
    String name

    /** The hub that defines / uses this Investment priority. */
    String hubId

    String description

    /** Categories are used to group investment priorities together */
    List categories

    /** The management units in which this priority occurs */
    List managementUnits

    Date dateCreated
    Date lastUpdated

    def beforeValidate() {
        if (investmentPriorityId == null) {
            investmentPriorityId = Identifiers.getNew(true, "")
        }
    }

    static mapping = {
        index (['name':1, 'status':1, 'categories':1], [unique: true])

    }

    static constraints = {
        description nullable: true
        type nullable: true
        name blank: false, unique: ['status']
        hubId nullable: true, validator: { String hubId, InvestmentPriority investmentPriority, Errors errors ->
            GormMongoUtil.validateWriteOnceProperty(investmentPriority, 'investmentPriorityId', 'hubId', errors)
        }
    }


}
