package au.org.ala.ecodata


import org.bson.types.ObjectId

/**
 * Describes the data and layout for a form that can be rendered to collect activity data.
 */
class ActivityForm {

    /** The list of properties to be used when binding request data to an ActivityForm */
    static bindingProperties = ['type', 'version', 'category', 'supportsSites', 'supportsPhotoPoints', 'gmsId', 'minOptionalSectionsCompleted', 'activationDate', 'sections', 'description']

    static mapWith = "mongo"

    static embedded = ['sections']

    static constraints = {
        name unique: ['formVersion']
        gmsId nullable: true
        category nullable: true
        activationDate nullable: true
        publicationStatus inList: [PublicationStatus.DRAFT, PublicationStatus.PUBLISHED]
        createdUserId nullable: true
        lastUpdatedUserId nullable: true
        minOptionalSectionsCompleted nullable: true
        description nullable: true
    }

    static mapping = {
        name index:true
        compoundIndex name:1, formVersion:-1
    }

    ObjectId id
    String status = Status.ACTIVE

    /** A unique name for this activity form */
    String name

    /** A description for this activity form */
    String description

    /** The purpose of this form - e.g. report, assessment */
    String type

    /** Used to group activity forms to help searching */
    String category

    /** Flags whether the data collected by this form can be associated with a Site or not */
    boolean supportsSites

    /** Flags whether this form should collect photo point information */
    boolean supportsPhotoPoints

    /** Legacy field for mapping the data in this form with the DoEE grants management system (now decomissioned) */
    String gmsId

    Integer minOptionalSectionsCompleted = null

    /**
     * Different to the version attribute used for optimistic locking as we don't want to increment
     * this version for every update.
     */
    Integer formVersion = 1

    /** Indicated whether this form is a draft or published */
    String publicationStatus = PublicationStatus.DRAFT

    /**
     * The date after which this version becomes available for use by an activity / report.
     * This is relative to the activity or report date rather than the date the
     * activity form is accessed by a user.  For example,  for 3rd quarter reporting we
     * want to switch to version 3 of the form, but earlier reports should still use version 2,
     * even if they are accessed/completed during quarter 3.
     */
    Date activationDate

    /**
     * A list of independently collapsible parts of this activity form, each with a description of the
     * data collected and the way it should be rendered.
     */
    List<FormSection> sections = []

    Date dateCreated
    Date lastUpdated

    String createdUserId
    String lastUpdatedUserId

    boolean isPublished() {
        return publicationStatus == PublicationStatus.PUBLISHED
    }

    void publish() {
        publicationStatus = PublicationStatus.PUBLISHED
    }

    void unpublish() {
        publicationStatus = PublicationStatus.DRAFT
    }

    private String currentUserId() {
        UserService.currentUser()?.userId ?: "<anon>"
    }

    def beforeInsert() {
        createdUserId = currentUserId()
        lastUpdatedUserId = currentUserId()
    }

    def beforeUpdate() {
        lastUpdatedUserId = currentUserId()
    }

    FormSection getFormSection(String name) {
        sections.find{it.name == name}
    }


}
