package au.org.ala.ecodata


import javax.persistence.PessimisticLockException

/**
 * Manages the pessimistic locking implementation used by MERIT.
 *
 * The expected usage pattern is:
 * 1. Obtain a lock on an entity by calling lock(entityId)
 * 2. When updating the entity use executeWithLock(entityId, updateClosure)
 * 3. After completing the updates call unlock(entityId)
 *
 * A lock is owned by a user (rather than a user session or generated ID).
 *
 * Users not holding the lock are allowed to call unlock.  This is so the lock can be released in unusual circumstances
 * such as crashes or people going on leave with update pages left open.
 *
 */
class LockService {

    UserService userService

    /**
     * Prerequisite: a user must exist and be obtainable by userService.getCurrentUserDetails()
     * This method will only execute the supplied action closure if the calling user either already holds a lock
     * on the entity with supplied id or if the entity is not already locked.  Otherwise a PessimisticLockException will be thrown.
     * This should be an exceptional case providing clients follow the standard locking pattern of obtaining a lock
     * explicitly before attempting an update.
     *
     * @param id identifies the entity that should be locked.
     * @param action a closure that will update the entity.
     * @return either returns the value the closure returns or throws a PessimisticLockException if the closure is not
     * called.
     */
    def executeWithLock(String id, Closure action) {
        boolean tempLock = false
        Lock lock = Lock.get(id)
        if (lock && !holdsLock(lock)) {
            throw new PessimisticLockException("Locked by "+lock.userId)
        }
        if (!lock) {
            tempLock = true
            try {
                this.lock(id)
            }
            catch (Exception e) {
                throw new PessimisticLockException("Failed to acquire lock for id: "+id)
            }
        }
        try {
            return action()
        }
        finally {
            if (tempLock) {
                this.unlock(id, false)
            }
        }

    }

    Lock get(String id) {
        Lock.get(id)
    }

    /**
     * Prerequisite: a user must exist and be obtainable by userService.getCurrentUserDetails()
     * Locks an entity.  The implementation uses the entityId as the primary key for the Lock to force
     * synchronization at the database level.  Locks are only inserted and deleted, never updated.
     * @param id the id of the entity to lock.
     * @return the Lock instance.  Throws an exception if the Lock already has been obtained.
     */
    Lock lock(String id) {
        def user = userService.getCurrentUserDetails()
        Lock lock = new Lock(id: id, userId: user.userId)
        Lock.withNewSession {
            lock.insert(flush: true, failOnError:true)
        }
        lock
    }

    boolean unlock(String id, Boolean force) {
        Lock lock = Lock.get(id)
        def user = userService.getCurrentUserDetails()

        boolean unlocked = false
        if (force || user?.userId == lock.userId) {
            unlocked = lock.delete(flush:true)
        }

        unlocked
    }

    boolean holdsLock(Lock lock) {
        lock.userId == userService.getCurrentUserDetails()?.userId
    }

    List<Lock> list(Integer max = 100, Integer offset = 0) {
        Lock.findAll([max: max, sort: "dateCreated", order: "asc", offset: offset])
    }
}
