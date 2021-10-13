package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class LockControllerSpec extends Specification implements ControllerUnitTest<LockController>, DataTest{

    LockService lockService = Mock(LockService)
    UserService userService = Mock(UserService)

    Class[] getDomainClassesToMock() {
        [Lock]
    }

    def setup() {
        controller.lockService = lockService
        controller.userService = userService

        Lock lock = new Lock()
        lock.id = '1'
        lock.userId = '1'
        Lock.withNewSession {
            lock.save(flush: true, failOnError:true)
        }
    }

    def cleanup() {
        Lock.findAll().each { it.delete(flush:true) }
    }

    void "Get lock"() {
        setup:

        when:
        params.id = '1'
        controller.get()

        then:
        response.status == HttpStatus.SC_OK
    }

    void "Lock"() {
        setup:

        when:
        params.id = '1'
        controller.lock()

        then:
        1 * lockService.lock('1') >> [status: 'ok']
        response.status == HttpStatus.SC_OK
    }

    void "unLock"() {
        setup:

        when:
        request.json = [force: false]
        controller.unlock('1')

        then:
        1 * lockService.unlock('1', false) >> [status: 'ok']
        response.status == HttpStatus.SC_OK
    }

    void "list"() {
        setup:

        when:
        controller.list()

        then:
        1 * lockService.list(null, null) >> [new Lock()]
        response.status == HttpStatus.SC_OK
    }
}
