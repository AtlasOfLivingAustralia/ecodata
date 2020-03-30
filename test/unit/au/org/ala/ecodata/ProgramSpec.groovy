package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

/**
 * Tests the mappings in the Program class.
 */

@TestMixin(MongoDbTestMixin)
@Domain(Program)
class ProgramSpec extends Specification {

    def setup() {
        Program.collection.remove(new BasicDBObject())
        UserService._currentUser.set(['userId':'1234'])
    }

    def cleanup() {
        Program.collection.remove(new BasicDBObject())
    }

    void "Associated sub-programs should be returned"() {

        setup:
        Program program = new Program(programId:'1', name:'Test', description:'test')
        program.save(flush:true, failOnError: true)
        Program subProgram = new Program(programId:'2', name:'Test 2', description:'test 2')
        subProgram.parent = program
        subProgram.save(flush:true, failOnError: true)


        when:
        Program found = Program.findByName('Test')

        then:
        found.name == 'Test'
        found.description == 'test'
        found.subPrograms.size() == 1
        found.subPrograms[0].name == 'Test 2'
    }


    void "program configuration is inheritable from parent programs"() {
        setup:
        Program p1 = new Program(programId:'p1', name: 'test 1', description: 'description 1', config:['a':'a', 'b':'b', c:'c'])
        p1.save(flush:true, failOnError: true)
        Program p2 = new Program(programId:'p2', name:'test 2', config:[a:'aa', d:'d'], parent: p1)
        p2.save(flush:true, failOnError: true)
        Program p3 = new Program(programId:'p3', name:'test 3', config:[b:'bb'], parent: p2)
        p3.save(flush:true, failOnError: true)

        when:
        Map config = p3.getInhertitedConfig()


        then:
        config.a == 'aa'
        config.b == 'bb'
        config.c == 'c'
        config.d == 'd'
    }

    void "when a program is retrieved, the parent program name and id are included"() {
        setup:
        Program p1 = new Program(programId:'p1', name: 'test 1', description: 'description 1', config:['a':'a', 'b':'b', c:'c'])
        p1.save(flush:true, failOnError: true)
        Program p2 = new Program(programId:'p2', name:'test 2', config:[a:'aa', d:'d'], parent: p1)
        p2.save(flush:true, failOnError: true)
        Program p3 = new Program(programId:'p3', name:'test 3', config:[b:'bb'], parent: p2)
        p3.save(flush:true, failOnError: true)

        when:
        Map serializedProgram = p3.toMap()

        then:
        serializedProgram.parent.name == p2.name
        serializedProgram.parent.programId == p2.programId

        serializedProgram.parent.parent.name == p1.name
        serializedProgram.parent.parent.programId == p1.programId

        when:
        serializedProgram = p1.toMap()

        then:
        serializedProgram.parent == null

    }

    void "Create Sub Program should added parent object"(){
        setup:
        Program program = new Program(programId:"123", name:"Parent program", description: "parent Description")
        program.save(flush: true, failOnError: true)

        when:
        Program parentProgram = Program.findByProgramId(program.programId)

        Program subPrograms = new Program(programId:"1213", name:"Sub Program", description: "Sub Description")
        subPrograms.parent = parentProgram
        subPrograms.save(flush: true, failOnError: true)


        then:
        subPrograms.programId == "1213"
        subPrograms.name == "Sub Program"
        subPrograms.description == "Sub Description"
        subPrograms.parent.id == parentProgram.id

    }
}
