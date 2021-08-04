package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification

class ProgramControllerSpec extends Specification implements ControllerUnitTest<ProgramController>, DataTest {

    ProgramService programService = Mock(ProgramService)

    def setup() {
        controller.programService = programService
        mockDomain Program
    }


    def "update requests only respond to PUT and POST requests"(String method, int result) {

        setup:
        request.method = method

        when:
        controller.update()

        then:
        response.status == result

        where:
        method | result
        'GET'  | HttpStatus.SC_METHOD_NOT_ALLOWED
        'POST' | HttpStatus.SC_NOT_FOUND // This is because we haven't mocked a response from the service (otherwise it would be OK)
        'PUT'  | HttpStatus.SC_NOT_FOUND // This is because we haven't mocked a response from the service (otherwise it would be OK)
        'DELETE' | HttpStatus.SC_METHOD_NOT_ALLOWED
     }


    def "delete requests only respond to DELETE requests"(String method, int result) {

        when:
        request.method = method
        controller.delete()

        then:
        response.status == result

        where:
        method | result
        'GET'  | HttpStatus.SC_METHOD_NOT_ALLOWED
        'POST' | HttpStatus.SC_METHOD_NOT_ALLOWED
        'PUT'  | HttpStatus.SC_METHOD_NOT_ALLOWED
        'DELETE' | HttpStatus.SC_NOT_FOUND // This is because we haven't mocked a response from the service (otherwise it would be OK)
    }


    void "A user can retrieve a list of programs they have a role assigned to"() {
        setup:
        String id = 'p1'
        List programs = [new Program(programId:'p1', name:'test 1'), new Program(programId:'p2', name:'test 2')]

        when:
        params.id = id
        controller.findAllForUser()

        then:
        1 * programService.findAllProgramsForUser(id) >> programs

        response.status == HttpStatus.SC_OK
        List responseData = response.json
        responseData.size() == 2
        responseData[0].programId == 'p1'
        responseData[0].name == 'test 1'
        responseData[1].programId == 'p2'
        responseData[1].name == 'test 2'
    }

    void "Get program by Id - with a deleted program"() {
        setup:
        request.method = 'GET'
        String id = 'p1'

        when:
        params.id = id
        controller.get()

        then:
        1 * programService.get(id, false) >> []

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.name == null
    }

    void "Get program by Id - with an active program"() {
        setup:
        request.method = 'GET'
        String id = 'p1'
        Program program = new Program(programId:'p1', name:'test 1')

        when:
        params.id = id
        controller.get()

        then:
        1 * programService.get(id, false) >> program

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.name == 'test 1'
    }

    void "Get programs by Ids"() {
        setup:
        List programs = [new Program(programId:'p1', name:'test 1', status: Status.ACTIVE), new Program(programId:'p2', name:'test 2', status: Status.DELETED)]
        List<String> ids = ['p1, p2']
        Map props = [
                programIds: ids
        ]

        when:
        request.method = 'POST'
        request.json = props
        controller.getPrograms()

        then:
        1 * programService.get(ids) >> programs

        response.status == HttpStatus.SC_OK
        List responseData = response.json as List
        responseData.size() == 2
        responseData[0].programId == 'p1'
        responseData[0].name == 'test 1'
        responseData[1].programId == 'p2'
        responseData[1].name == 'test 2'
    }

    void "Find program by name - with a deleted program"() {
        setup:
        String name = 'test1'

        when:
        request.method = 'GET'
        params.name = name
        controller.findByName()

        then:
        1 * programService.findByName(name) >> []

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.name == null
    }

    void "Find program by name - without a deleted program"() {
        setup:
        String name = 'test1'
        Program program = new Program(programId:'p1', name:'test 1')

        when:
        request.method = 'GET'
        params.name = name
        controller.findByName()

        then:
        1 * programService.findByName(name) >> program

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.name == 'test 1'
    }

    void "Create program"() {
        setup:
        Map props = [
                programId:'p1', name:'test 1'
        ]
        Program program = new Program(programId:'p1', name:'test 1', status: Status.ACTIVE)

        when:
        request.method = 'POST'
        request.json = props
        controller.update(null)

        then:
        1 * programService.create(props) >> program

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.name == 'test 1'
        responseData.programId == 'p1'
        responseData.status == Status.ACTIVE
    }

    void "Update program"() {
        setup:
        Map props = [
                programId:'p1', name:'test 1'
        ]
        Program program = new Program(programId:'p1', name:'test 1', status: Status.ACTIVE)

        when:
        request.method = 'PUT'
        request.json = props
        controller.update('p1')

        then:
        1 * programService.update('p1', props) >> program

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.name == 'test 1'
        responseData.programId == 'p1'
        responseData.status == Status.ACTIVE
    }

    void "Delete program"() {
        setup:
        request.method = 'DELETE'
        String id = 'p1'

        when:
        params.id = id
        controller.delete()

        then:
        1 * programService.delete(id, false) >> [status: 'ok']

        response.status == HttpStatus.SC_OK
        Program responseData = response.json as Program
        responseData.status == 'ok'
    }

    void "Get programs list"() {
        setup:
        List programs = [new Program(programId:'p1', name:'test 1'), new Program(programId:'p2', name:'test 2')]

        when:
        request.method = 'GET'
        controller.listOfAllPrograms()

        then:
        1 * programService.findAllProgramList() >> programs

        response.status == HttpStatus.SC_OK
        List responseData = response.json as List
        responseData.size() == 2
        responseData[0].programId == 'p1'
        responseData[0].name == 'test 1'
        responseData[1].programId == 'p2'
        responseData[1].name == 'test 2'
    }

}
