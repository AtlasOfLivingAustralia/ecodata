package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec

import static com.mongodb.client.model.Filters.eq

class UserSpec extends MongoSpec {

    def setup() {
        User.findAll().each{it.delete(flush:true)}
    }

    def cleanup() {
        User.findAll().each{it.delete(flush:true)}
    }

    def "The user collection can be queried on the embedded hubLogins association"() {
        setup:
        String hubId1 = "h1"
        String hubId2 = "h2"
        Date date = DateUtil.parse("2021-07-01T00:00:00Z")
        new User(userId:"1", userHubs: [new UserHub(hubId:hubId1, lastLogin: date)]).save(flush:true, failOnError:true)
        new User(userId:"2", userHubs: [new UserHub(hubId:hubId1, lastLogin:null), new UserHub(hubId:hubId2, lastLogin: date)]).save(flush:true, failOnError:true)
        new User(userId:"3", userHubs: [new UserHub(hubId:hubId2, lastLogin:null)]).save(flush:true, failOnError:true)

        int count = 0

        when:
        User.find(eq('userHubs.hubId', hubId1)).each {count++ }

        then:
        count == 2

        when:
        List users = User.where {
            userHubs {
                hubId == hubId2
            }
        }.list()

        then:
        users.size() == 2

        when:
        users = User.findAllByLoginHub(hubId1, [max:100, offset:0])

        then:
        users.size() == 2
    }

    void "the userId should be a unique field"() {
        setup: "We have an existing entry for userId = 1"
        new User(userId:"1").save(flush:true, failOnError:true)

        when:
        User user = new User(userId:"1")
        user.save()

        then:
        user.hasErrors()

    }

    def "the hubId on the embedded UserHub object should be a unique field"() {
        setup: "We have an existing entry for userId = 1"
        new User(userId:"1", userHubs: [new UserHub(hubId:'1')]).save(flush:true, failOnError:true)

        when:
        User user = User.findByUserId("1")
        user.userHubs << new UserHub(hubId:'1')
        user.save()

        then:
        user.hasErrors()
        user.errors.getFieldError('userHubs').code == 'user.userHubs.hubId.unique'

    }

    def "A UserHub can be retrieved by hubId from a User"() {
        setup:
        Date date = DateUtil.parse("2021-07-01T00:00:00Z")
        User user = new User(userId:"2", userHubs: [new UserHub(hubId:"h1", lastLoginTime:null), new UserHub(hubId:"h2", lastLoginTime: date)])

        expect:
        user.getUserHub("h1").lastLoginTime == null
        user.getUserHub("h2").lastLoginTime == date
        user.getUserHub("h3") == null
    }

    def "The User can record a login to a hub"() {
        setup:
        Date date = DateUtil.parse("2021-07-01T00:00:00Z")
        User user = new User(userId:"2", userHubs: [new UserHub(hubId:"h1", lastLoginTime:null), new UserHub(hubId:"h2", lastLoginTime: date)])

        when:
        user.loginToHub("h1", date)

        then:
        user.getUserHub("h1").lastLoginTime == date

        when:
        user.loginToHub("h3", date)

        then:
        user.getUserHub("h3").lastLoginTime == date
    }
}
