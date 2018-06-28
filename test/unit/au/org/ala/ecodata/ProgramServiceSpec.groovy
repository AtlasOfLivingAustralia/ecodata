package au.org.ala.ecodata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.springframework.context.MessageSource
import spock.lang.Specification

@TestMixin(MongoDbTestMixin)
@Domain(Program)
@TestFor(ProgramService)
class ProgramServiceSpec extends Specification {

    CommonService commonService = new CommonService()

    def setup() {
        commonService.grailsApplication = grailsApplication
        commonService.messageSource = Mock(MessageSource)
        service.commonService = commonService

        Program.findAll().each { it.delete(flush:true) }
    }

    def cleanup() {
        Program.findAll().each { it.delete(flush:true) }
    }

    void "insert a new program"() {
        setup:
        Map programDetails = [name: 'test 1', description: 'description 1']
        when:
        Program program = service.create(programDetails)
        program = Program.findByProgramId(program.programId)

        then:
        program.name == 'test 1'
        program.description == 'description 1'
    }

    void "update an existing program"() {
        setup:
        String programId = '1'
        Program program = new Program(programId:programId, name: 'test 1', description: 'description 1')
        program.save(flush:true, failOnError: true)

        when:
        Map newDetails = [name:'test 2', description: 'description 2']
        service.update(programId, newDetails)
        Program newProgram = service.get(programId)

        then:
        newProgram.name == 'test 2'
        newProgram.description == 'description 2'
    }
}
