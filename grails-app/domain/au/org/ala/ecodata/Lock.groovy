package au.org.ala.ecodata

/**
 * Holds a pessimistic lock for an entity.
 * Designed to be inserted / deleted only so we can use mongo's atomic operations to ensure the lock is set.
 * (i.e if two threads attempt to lock the same entity at the same time, one insert will succeed, the other will fail.)
 */
class Lock {

    static constraints = {
        userId index:true
        entityType nullable: true
    }

    static mapping = {
        id generator: 'assigned'
    }

    void setId(String id) {
        this.id = id
    }

    /** The ID of the entity to lock */
    String id

    /** The type of entity being locked. */
    String entityType
    /** When the lock was created */
    Date dateCreated

    /** The id of the user who holds the lock */
    String userId
}
