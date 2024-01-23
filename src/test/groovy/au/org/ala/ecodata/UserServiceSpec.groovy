package au.org.ala.ecodata

import au.org.ala.web.AuthService
import au.org.ala.ws.security.client.AlaOidcClient
import au.org.ala.ws.security.profile.AlaOidcUserProfile
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.pac4j.core.config.Config
import org.pac4j.core.credentials.AnonymousCredentials
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.profile.UserProfile
import spock.lang.Unroll

/**
 * We are extending the mongo spec as one of the main things we need to test are complex queries on
 * the User collection, which are potentially different in GORM for Mongo vs GORM
 */
class UserServiceSpec extends MongoSpec implements ServiceUnitTest<UserService> {

    WebService webService = Mock(WebService)
    AuthService authService = Mock(AuthService)
    AlaOidcClient alaOidcClient
    Config pack4jConfig

    def setup() {
        User.findAll().each{it.delete(flush:true)}
        Hub.findAll().each{it.delete(flush:true)}
        new Hub(hubId:'h1', urlPath:"hub1").save(flush:true, failOnError:true)
        new Hub(hubId:'h2', urlPath:'hub2').save(flush:true, failOnError:true)
        service.webService = webService
        service.authService = authService
    }

    def cleanup() {
        User.findAll().each{it.delete(flush:true)}
        Hub.findAll().each{it.delete(flush:true)}
    }

    def "The recordLoginTime method requires a hubId and userId to be supplied"() {
        when:
        service.recordUserLogin(null, "user1")

        then:
        thrown(IllegalArgumentException)

        when:
        service.recordUserLogin("hub1", null)

        then:
        thrown(IllegalArgumentException)
    }

    def "The Hub associated with the supplied hubId must exist"() {
        when:
        service.recordUserLogin("h3", "user1")

        then:
        thrown(IllegalArgumentException)
    }

    def "The service provides a convenience method to record the time a user logged into a hub"() {
        setup:
        String hubId = "h1"
        String userId = "u1"
        new Hub(hubId:hubId).save()
        Date loginTime1 = DateUtil.parse("2021-01-01T00:00:00Z")
        Date loginTime2 = DateUtil.parse("2021-01-01T00:00:00Z")

        when: "No User document exists, this method will insert one"
        User user = service.recordUserLogin(hubId, userId, loginTime1)

        then:
        user.userId == userId
        user.getUserHub(hubId).hubId == hubId
        user.getUserHub(hubId).lastLoginTime == loginTime1

        when: "If the hub doesn't exist, the method will add one"
        user = service.recordUserLogin("h2", userId, loginTime2)

        then:
        user.userId == userId
        user.userHubs.size() == 2
        user.getUserHub("h2").lastLoginTime == loginTime2

        when: "The last login time is changed, it is updated correctly"
        user = service.recordUserLogin(hubId, userId, loginTime2)

        then:
        user.userId == userId
        user.userHubs.size() == 2
        user.getUserHub(hubId).lastLoginTime == loginTime2
        user.getUserHub("h2").lastLoginTime == loginTime2

    }

    @Unroll
    def "The service can return a list of users who haven't logged into a hub after a specified time"(String hubId, String queryDate, int expectedResultCount) {
        setup:
        insertUserLogin("u1", "h1", "2021-01-01T00:00:00Z")
        insertUserLogin("u1", "h2", "2021-02-01T00:00:00Z")
        insertUserLogin("u2", "h1", "2021-01-10T00:00:00Z")
        insertUserLogin("u3", "h1", "2021-05-01T00:00:00Z")
        insertUserLogin("u4", "h1", "2021-06-01T00:00:00Z")
        User.withSession {session -> session.flush()}

        when:
        Date date = DateUtil.parse(queryDate)
        List users = service.findUsersNotLoggedInToHubSince(hubId, date)
        List users2 = User.findAll()

        then:
        users2.size() == 4
        users2[0].userId == "u1"
        users2[0].userHubs.size() == 2
        users2[0].getUserHub("h1") != null
        users2[0].getUserHub("h2") != null

        users.size() == expectedResultCount

        where:
        hubId | queryDate              | expectedResultCount
        "h1"  | "2021-02-01T00:00:00Z" | 2
        "h2"  | "2021-02-01T00:00:00Z" | 0
        //"h2"  | "2021-02-02T00:00:00Z" | 1 currently failing in travis only
        "h1"  | "2021-05-15T00:00:00Z" | 3
    }

    @Unroll
    def "The service can return a list of users who need to be warned their access is due to expire"(String hubId, String fromDateStr, String toDateStr, int expectedResultCount) {
        setup:
        insertUserLogin("u1", "h1", "2021-01-01T00:00:00Z")
        insertUserLogin("u1", "h2", "2021-02-01T00:00:00Z")
        insertUserLogin("u2", "h1", "2021-01-10T00:00:00Z")
        insertUserLogin("u3", "h1", "2021-05-01T00:00:00Z")
        insertUserLogin("u4", "h1", "2021-06-01T00:00:00Z")
        User.withSession {session -> session.flush()}

        when:
        Date fromDate = DateUtil.parse(fromDateStr)
        Date toDate = DateUtil.parse(toDateStr)
        List users = service.findUsersWhoLastLoggedInToHubBetween(hubId, fromDate, toDate)

        then:
        users.size() == expectedResultCount

        where:
        hubId | fromDateStr            |  toDateStr             | expectedResultCount
        "h1"  | "2021-01-01T00:00:00Z" | "2021-02-01T00:00:00Z" | 2
        "h2"  | "2021-01-01T00:00:00Z" | "2021-02-01T00:00:00Z" | 0
        //"h2"  | "2021-02-01T00:00:00Z" | "2021-03-01T00:00:00Z" | 1 currently failing in travis only
        "h1"  | "2021-04-15T00:00:00Z" | "2021-05-01T00:00:00Z" | 0
    }

    def "The service uses a webservice and the auth service to authorize a mobile user"() {
        setup:
        String username = "user"
        String authKey = "1234"

        when:
        String userId = service.authorize(username, authKey)

        then:
        1 * webService.doPostWithParams({it.endsWith('/mobileauth/mobileKey/checkKey')}, [userName:username, authKey:authKey], true) >> [resp:[status:'success']]
        1 * authService.getUserForUserId(username) >> [userId:'u1']

        and:
        userId == 'u1'
    }

    def "An empty result is returned if a mobile user doesn't specify a username or authKey"() {
        setup:
        String username = "user"
        String authKey = "1234"

        expect:
        !service.authorize("", authKey)
        !service.authorize(username, "")
        !service.authorize(null, null)

    }
    def "An empty result is returned if a mobile user cannot be authorized"() {
        setup:
        String username = "user"
        String authKey = "1234"

        when:
        String userId = service.authorize(username, authKey)

        then:
        1 * webService.doPostWithParams({it.endsWith('/mobileauth/mobileKey/checkKey')}, [userName:username, authKey:authKey], true) >> [resp:[statusCode:403]]
        0 * authService._

        and:
        !userId

        when:
        userId = service.authorize(username, authKey)

        then:
        1 * webService.doPostWithParams({it.endsWith('/mobileauth/mobileKey/checkKey')}, [userName:username, authKey:authKey], true) >> [resp:[status:'success']]
        1 * authService.getUserForUserId(username) >> null

        and:
        !userId
    }

    void "getUserFromJWT returns user when Authorization header is passed"() {
        setup:
        def result
        alaOidcClient = GroovyMock([global: true], AlaOidcClient)
        pack4jConfig =  GroovyMock([global: true], Config)
        service.alaOidcClient = alaOidcClient
        service.config = pack4jConfig
        AlaOidcUserProfile person = new AlaOidcUserProfile(user.userId)
        Optional<Credentials> credentials = new Optional<Credentials>(AnonymousCredentials.INSTANCE)
        Optional<UserProfile> userProfile = new Optional<UserProfile>(person)

        when:
        request.addHeader('Authorization', 'Bearer abcdef')
        result = service.getUserFromJWT()

        then:
        alaOidcClient.getCredentials(*_) >> credentials
        alaOidcClient.getUserProfile(*_) >> userProfile
        authService.getUserForUserId(user.userId)  >> userDetails
        result.userName == user.userName
        result.displayName == "${user.firstName} ${user.lastName}"
        result.userId == user.userId
    }


    private void insertUserLogin(String userId, String hubId, String loginTime) {
        Date date = DateUtil.parse(loginTime)
        User user = service.recordUserLogin(hubId, userId, date)
        if (user.hasErrors()) {
            throw new Exception(user.errors.toString())
        }
    }

}
