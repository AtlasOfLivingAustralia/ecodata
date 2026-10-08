package au.org.ala.ecodata

import au.org.ala.ws.tokens.TokenService
import com.nimbusds.oauth2.sdk.token.AccessToken
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class WebServiceSpec extends Specification implements ServiceUnitTest<WebService> {

    TokenService tokenService = Mock(TokenService)
    AccessToken accessToken = Mock(AccessToken)

    void setup() {
        service.tokenService = tokenService
        service.WHITE_LISTED_DOMAINS = ['ala.org.au', 'localhost']
    }

    void "canAddSecret returns true for whitelisted ALA domains"() {
        expect:
        service.canAddSecret('https://collections-test.ala.org.au/ws/dataResource')
        service.canAddSecret('https://images.test.ala.org.au/ws/uploadImage')
        service.canAddSecret('https://spatial.test.ala.org.au/ws/shape/upload/geojson')
        service.canAddSecret('https://ala.org.au/test')
        service.canAddSecret('http://localhost:8080/test')
    }

    void "canAddSecret returns false for non-whitelisted domains"() {
        expect:
        !service.canAddSecret('https://example.com/test')
        !service.canAddSecret('https://notala.org.au/test')
        !service.canAddSecret('https://ala.org.au.example.com/test')
    }

    void "canAddSecret returns false for invalid URLs"() {
        expect:
        !service.canAddSecret('it-is-an-invalid-url')
    }

    void "getAuthTokenForUrl returns authorization header for whitelisted URL"() {
        setup:
        tokenService.getAuthToken(false) >> accessToken
        accessToken.toAuthorizationHeader() >> 'Bearer test-token'

        when:
        String result = service.getAuthTokenForUrl(
            'https://images.test.ala.org.au/ws/uploadImage'
        )

        then:
        result == 'Bearer test-token'
    }

    void "getAuthTokenForUrl does not request token for non-whitelisted URL"() {
        when:
        String result = service.getAuthTokenForUrl(
            'https://example.com/test'
        )

        then:
        result == null
        0 * tokenService.getAuthToken(_)
    }

    void "getAuthTokenForUrl can require a user token"() {
        setup:
        tokenService.getAuthToken(true) >> accessToken
        accessToken.toAuthorizationHeader() >> 'Bearer user-token'

        when:
        String result = service.getAuthTokenForUrl(
            'https://collections-test.ala.org.au/ws/dataResource',
            true
        )

        then:
        result == 'Bearer user-token'
    }
}