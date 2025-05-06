package au.org.ala.ecodata

import org.bson.types.ObjectId
import org.springframework.validation.Errors

/**
 * A Term represents an element of a vocabulary or constrained list of values.
 * Uses include:
 * - A list of tags available for a MERIT project
 * - A list of first nations labels for NESP projects and documents
 * - A list of values for selection from a dropdown in a report/activity form.
 * */
class Term {

    ObjectId id

    String termId
    String status = Status.ACTIVE

    /** Identifies the term */
    String term

    /** The hub that defines / uses this Term. */
    String hubId

    /** Discriminators to support Term label overrides */
    String entityType
    String entityId

    String description

    /** Allows a term to be overridden by a Project */
    String label

    /** Categories are used to group terms together */
    String category

    /** If there is an icon associated with this term, this is the URL to that icon */
    String iconUrl

    Date dateCreated
    Date lastUpdated

    def beforeValidate() {
        if (termId == null) {
            termId = Identifiers.getNew(true, "")
        }
    }

    static mapping = {
        index (['hubId':1, 'status':1, 'category':1, 'term':1], [unique: true])

    }

    static constraints = {
        description nullable: true
        label nullable: true
        iconUrl nullable: true
        entityType nullable: true
        entityId nullable: true
        term blank: false, unique: ['hubId', 'status', 'category']
        hubId nullable: true, validator: { String hubId, Term term, Errors errors ->
            GormMongoUtil.validateWriteOnceProperty(term, 'termId', 'hubId', errors)
        }
    }


}
