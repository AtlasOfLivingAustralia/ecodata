package au.org.ala.ecodata

/**
 * Indicates the workflow state of an entity.
 */
interface PublicationStatus {
    String DRAFT = "unpublished"
    String SUBMITTED_FOR_REVIEW = "pendingApproval"
    String PUBLISHED = "published"
}