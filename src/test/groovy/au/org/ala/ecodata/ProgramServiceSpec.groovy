package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.apache.http.HttpStatus
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
        UserPermission.findAll().each { it.delete(flush:true) }
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

    void "Get including deleted"() {
        setup:
        Program p = new Program(programId: "1", name: 'Program Name', description: 'description', status: Status.DELETED)
        p.save(flush: true, failOnError: true)

        when:
        Program responseProgram = service.get('1', true)

        then:
        responseProgram != null
        responseProgram.programId == '1'
    }

    void "Get by ids"() {
        setup:
        Program p1 = new Program(programId: "1", name: 'Program Name1', description: 'description', status: Status.DELETED)
        p1.save(flush: true, failOnError: true)
        Program p2 = new Program(programId: "2", name: 'Program Name2', description: 'description', status: Status.ACTIVE)
        p2.save(flush: true, failOnError: true)
        String[] ids = ['1', '2']

        when:
        Program[] responsePrograms = service.get(ids)

        then:
        responsePrograms != null
        responsePrograms.size() == 2
    }

    void "Find by name"() {
        setup:
        Program p = new Program(programId: "1", name: 'Program Name', description: 'description', status: Status.ACTIVE)
        p.save(flush: true, failOnError: true)

        when:
        Program responseProgram = service.findByName('Program Name')

        then:
        responseProgram != null
        responseProgram.programId == '1'
    }

    void "get parent names"() {
        Program parentProgram = new Program(programId: "1", name: 'parentProgram Name', description: 'parent description')
        parentProgram.save(flush: true, failOnError: true)

        Program subProgram = new Program(programId: "2", name: "subprogram name", description: "subProgram description")
        Program parent = service.get("1")
        subProgram.parent = parent
        subProgram.save(flush: true, failOnError: true)

        when:
        def names = service.parentNames(subProgram)

        then:
        names != null
        names.size() == 2
        names[0] == 'subprogram name'
        names[1] == 'parentProgram Name'
    }

    void "Find all programs for user"() {
        setup:
        Program p = new Program(programId: "1", name: 'Program Name', description: 'description', status: Status.DELETED)
        p.save(flush: true, failOnError: true)
        new UserPermission(userId:'1', entityId:'1', entityType:Program.name, accessLevel: AccessLevel.admin).save(flush:true, failOnError:true)

        when:
        def programIds = service.findAllProgramsForUser('1')

        then:
        programIds != null
        programIds.size() == 1
        programIds[0].name == 'Program Name'
    }

    void "Find all"() {
        setup:
        Program p1 = new Program(programId: "1", name: 'Program Name1', description: 'description', status: Status.DELETED)
        p1.save(flush: true, failOnError: true)
        Program p2 = new Program(programId: "2", name: 'Program Name2', description: 'description', status: Status.ACTIVE)
        p2.save(flush: true, failOnError: true)
        Program p3 = new Program(programId: "3", name: 'Program Name3', description: 'description', status: Status.ACTIVE, parent: p2)
        p3.save(flush: true, failOnError: true)

        when:
        def programList = service.findAllProgramList()

        then:
        programList != null
        programList.size() == 2
        programList[0].programId == '2'
        programList[0].name =='Program Name2'
        !programList[0].parentId
        !programList[0].parentName
        programList[1].programId == '3'
        programList[1].name =='Program Name3'
        programList[1].parentId == '2'
        programList[1].parentName == 'Program Name2'

    }

    void "delete"() {
        setup:
        Program p = new Program(programId: "1", name: 'Program Name', description: 'description', status: Status.ACTIVE)
        p.save(flush: true, failOnError: true)

        when:
        def response = service.delete('1', false)

        then:
        response.status == 'ok'
    }

    void "delete - destroy"() {
        setup:
        Program p = new Program(programId: "1", name: 'Program Name', description: 'description', status: Status.ACTIVE)
        p.save(flush: true, failOnError: true)

        when:
        def response = service.delete('1', true)

        then:
        response.status == 'ok'
    }

    void "delete - invalid id"() {
        setup:
        Program p = new Program(programId: "1", name: 'Program Name', description: 'description', status: Status.ACTIVE)
        p.save(flush: true, failOnError: true)

        when:
        def response = service.delete('2', false)

        then:
        response.status == 'error'
        response.errors[0] == 'No such id'
    }
}
