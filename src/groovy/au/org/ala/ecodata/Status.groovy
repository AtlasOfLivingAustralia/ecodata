package au.org.ala.ecodata

/**
 * Tracks the status of an entity.  Mostly used to implement soft deletes, however Projects
 * have additional statuses that track their lifecycle.
 */
interface Status {
    String ACTIVE = "active"
    String COMPLETED = "completed"
    String DELETED = "deleted"
    /** New MERIT projects are loaded as "applications" and move to active when approved */
    String APPLICATION = "application"
    /** This status distinguishes projects that completed abnormally from the COMPLETED status.  Used by MERIT projects */
    String TERMINATED = "terminated"
}