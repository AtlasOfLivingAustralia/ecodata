package au.org.ala.ecodata

import grails.test.mixin.TestFor
import org.apache.http.HttpStatus
import spock.lang.Specification

@TestFor(ProgramController)
class ProgramControllerSpec extends Specification {

    ProgramService programService = Mock(ProgramService)

    def setup() {
        controller.programService = programService
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


}
