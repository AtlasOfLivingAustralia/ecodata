package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.test.mongodb.MongoSpec

class ManagementUnitSpec extends MongoSpec {

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
