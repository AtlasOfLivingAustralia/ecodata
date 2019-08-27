package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import spock.lang.Specification

import static au.org.ala.ecodata.Status.DELETED

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(MongoDbTestMixin)
@Domain(ManagementUnit)
class ManagementUnitSpec extends Specification {

    def setup() {
        ManagementUnit.collection.remove(new BasicDBObject())
        UserService._currentUser.set(['userId':'1234'])
    }

    def cleanup() {
        ManagementUnit.collection.remove(new BasicDBObject())
    }

    void "GORM test"() {
        setup:
        ManagementUnit p1 = new ManagementUnit(managementUnitId: 'p1', name: 'test 1', description: 'description 1',status:Status.ACTIVE)
        p1.save(flush:true, failOnError: true)

        when:
        ManagementUnit result = ManagementUnit.findByManagementUnitId('p1')
        ManagementUnit result1 = ManagementUnit.findByManagementUnitIdAndStatusNotEqual('p1', Status.DELETED)

        then:
        result.name=='test 1'
        result1.name=='test 1'


    }
}
