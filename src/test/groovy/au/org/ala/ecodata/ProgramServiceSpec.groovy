package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest

import org.springframework.context.MessageSource
import spock.lang.Specification

class ProgramServiceSpec extends MongoSpec implements ServiceUnitTest<ProgramService> {

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

    void "update existing Parent with new parent program"(){
        setup:

        Program parentProgram = new Program(programId: "1", name: 'parentProgram Name', description: 'parent description')
        parentProgram.save(flush: true, failOnError: true)

        Program subProgram = new Program(programId: "2", name: "subprogram name", description: "subProgram description")
        Program parent = service.get("1")
        subProgram.parent = parent
        subProgram.save(flush: true, failOnError: true)

        Program newParentProgram = new Program(programId: '3', name: 'new Parent Program Name', description: 'new Parent Program Description')
        newParentProgram.save(flush: true, failOnError: true)

        when:
        Map details = [programId: "2", name: "subprogram name", description: "subProgram description", parentProgramId:'3']
        service.update(details.programId, details)
        Program updatedProgram = service.get(subProgram.programId)

        then:
        updatedProgram.name == "subprogram name"
        updatedProgram.description == "subProgram description"
        updatedProgram.parent.id == newParentProgram.id
    }

    void "update existing Parent with null parent program"(){
        setup:

        Program parentProgram = new Program(programId: "1", name: 'parentProgram Name', description: 'parent description')
        parentProgram.save(flush: true, failOnError: true)

        Program subProgram = new Program(programId: "2", name: "subprogram name", description: "subProgram description")
        Program parent = service.get("1")
        subProgram.parent = parent
        subProgram.save(flush: true, failOnError: true)

        when:
        Map details = [programId: "2", name: "subprogram name", description: "subProgram description", parentProgramId: null]
        service.update(details.programId, details)
        Program updatedProgram = service.get(subProgram.programId)

        then:
        updatedProgram.name == "subprogram name"
        updatedProgram.description == "subProgram description"
        updatedProgram.parent == null
    }

    void "If no parentProgramId is supplied to an update, the Program parent should remain unchanged"() {
        Program parentProgram = new Program(programId: "1", name: 'parentProgram Name', description: 'parent description')
        parentProgram.save(flush: true, failOnError: true)

        Program subProgram = new Program(programId: "2", name: "subprogram name", description: "subProgram description")
        Program parent = service.get("1")
        subProgram.parent = parent
        subProgram.save(flush: true, failOnError: true)

        when:
        Map details = [programId: "2", name: "subprogram name", description: "edited subProgram description"]
        service.update(details.programId, details)
        Program updatedProgram = service.get(subProgram.programId)

        then:
        updatedProgram.name == details.name
        updatedProgram.description == details.description
        updatedProgram.parent.id == parentProgram.id
    }

}
