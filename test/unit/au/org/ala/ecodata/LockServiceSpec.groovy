package au.org.ala.ecodata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

import javax.persistence.PessimisticLockException

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(LockService)
@TestMixin(MongoDbTestMixin)
@Domain(Lock)
class LockServiceSpec extends Specification {

    def userService
    def setup() {
        Lock.get('id')?.delete(flush:true)
        userService = Stub(UserService)
        service.userService = userService
    }

    def cleanup() {
        Lock.get('id')?.delete(flush:true)
    }

    void "a lock can only be obtained once"() {

        setup:

        userService.getCurrentUserDetails() >> new UserDetails(userId:'user')
        when:
        Lock lock = service.lock('id')

        then:
        lock.userId == 'user'
        lock.id == 'id'

        when:
        service.lock('id')
        then:
        DataIntegrityViolationException ex = thrown()
        ex.message.contains("lock")

    }

    void "a lock can be released once obtained"() {

        setup:

        userService.getCurrentUserDetails() >> new UserDetails(userId:'user')
        when:
        Lock lock = service.lock('id')

        then:
        lock.userId == 'user'
        lock.id == 'id'

        when:
        service.unlock('id', false)
        then:
        Lock.get('id') == null

    }

    void "a lock can be released by a different user to the one that obtained it"() {

        setup:
        UserDetails user = new UserDetails(userId:"user")
        userService.getCurrentUserDetails() >> user
        when:
        Lock lock = service.lock('id')

        then:
        lock.userId == 'user'
        lock.id == 'id'

        when:
        user.userId = "user2"
        service.unlock('id', true)

        then:
        Lock.get('id') == null

    }


    void "an executeWithLock block can be executed by a user holding the lock"() {

        setup:
        userService.getCurrentUserDetails() >> new UserDetails(userId:'user')
        service.lock('id')
        boolean executed = false

        when:
        service.executeWithLock('id', { executed = true})

        then:
        executed == true
        and: "The lock is still held by the user"
        Lock.get('id')?.id == 'id'

    }

    void "an executeWithLock block cannot be executed when another user holds the lock"() {

        setup:
        UserDetails user = new UserDetails(userId:"user")

        userService.getCurrentUserDetails() >> user
        service.lock('id')
        boolean executed = false

        when:
        user.userId = "user2"
        service.executeWithLock('id', { executed = true})

        then:
        executed == false
        PessimisticLockException ex = thrown()
        ex != null


    }

    void "an executeWithLock block can be executed when the entity is not locked"() {

        setup:
        userService.getCurrentUserDetails() >> new UserDetails(userId:'user')
        boolean executed = false

        when:
        service.executeWithLock('id', { executed = true})

        then:
        executed == true
        and: "The lock will be released at the end of the block"
        Lock.get('id') == null
    }

}
