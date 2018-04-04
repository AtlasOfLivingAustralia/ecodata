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
}
