package au.org.ala.ecodata

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import geb.Browser
import spock.lang.Shared
import wiremock.com.github.jknack.handlebars.EscapingStrategy
import wiremock.com.github.jknack.handlebars.Handlebars
import wiremock.com.github.jknack.handlebars.Helper
import wiremock.com.github.jknack.handlebars.Options
import wiremock.com.google.common.collect.ImmutableMap

import static com.github.tomakehurst.wiremock.client.WireMock.*

/**
 * Supports stubbing access to CAS via wiremock.
 */
class StubbedCasSpec extends FunctionalTestHelper {

    @Shared WireMockServer wireMockServer
    def setupSpec() {

        Handlebars handlebars = new Handlebars()
        handlebars.escapingStrategy = EscapingStrategy.NOOP

        // This is done so we can use a custom handlebars with a NOOP escaping strategy - the default escapes HTML
        // which breaks the redirect URL returned by the PDF generation stub.
        Helper noop = new Helper() {
            Object apply(Object context, Options options) throws IOException {
                return context[0]
            }
        }
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .port(testConfig.wiremock.port)
                .usingFilesUnderDirectory(getMappingsPath())
                .extensions(new ResponseTemplateTransformer(false, handlebars, ImmutableMap.of("noop", noop),null, null)))

        wireMockServer.start()

        // Configure the client
        configureFor("localhost", testConfig.wiremock.port)
    }

    def cleanupSpec() {
        wireMockServer.stop()
    }

    /**
     * Opens a new window and logs out.  This will cause the next
     * request to be unauthenticated which is a reasonable simulation of
     * a session timeout.
     */
    def simulateTimeout(Browser browser) {
        withNewWindow({
            js.exec("window.open('.');")},
                {logout(browser); return true})
    }

    /** Presses the OK button on a displayed bootbox modal */
    def okBootbox() {
        $('.bootbox .btn-primary').each { ok ->


            waitFor 20, {
                try {
                    if (ok.displayed) {
                        ok.click()
                    }

                }
                catch (Exception e) {
                    e.printStackTrace()
                }
                waitFor {
                    $('.modal-backdrop').size() == 0
                }
            }

        }
    }

    private static String getMappingsPath() {
        new File("src/integration-test/resources/wiremock")
    }

    def login(Map userDetails, Browser browser) {

        String email = "fc-te@outlook.com"

        List roles = ["ROLE_USER"]
        if (userDetails.role) {
            roles << userDetails.role
        }
        String casRoles = ""
        roles.each { role ->
            casRoles += "<cas:role>${role}</cas:role>"
        }

        String casXml = """
<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
    <cas:authenticationSuccess>
        <cas:user>${userDetails.email}</cas:user>
        <cas:attributes>
            <cas:lastLogin>2019-08-19 06:25:31</cas:lastLogin>
            <cas:country>AU</cas:country>
            <cas:firstname>${userDetails.firstName}</cas:firstname>
            ${casRoles}
            <cas:role>ROLE_USER</cas:role>
            <cas:isFromNewLogin>false</cas:isFromNewLogin>
            <cas:authenticationDate>2019-08-19T06:34:15.495Z[UTC]</cas:authenticationDate>
            <cas:city></cas:city>
            <cas:clientName>Google</cas:clientName>
            <cas:created>2012-01-05 01:11:19</cas:created>
            <cas:givenName>${userDetails.firstName}</cas:givenName>
            <cas:successfulAuthenticationHandlers>ClientAuthenticationHandler</cas:successfulAuthenticationHandlers>
            <cas:organisation>${userDetails.organisation ?: ''}</cas:organisation>
            <cas:userid>${userDetails.userId}</cas:userid>
            <cas:lastname>${userDetails.lastName}</cas:lastname>
            <cas:samlAuthenticationStatementAuthMethod>urn:oasis:names:tc:SAML:1.0:am:unspecified</cas:samlAuthenticationStatementAuthMethod>
            <cas:credentialType>ClientCredential</cas:credentialType>
            <cas:authenticationMethod>ClientAuthenticationHandler</cas:authenticationMethod>
            <cas:authority>${roles.join(',')}</cas:authority>
            <cas:longTermAuthenticationRequestTokenUsed>false</cas:longTermAuthenticationRequestTokenUsed>
            <cas:state></cas:state>
            <cas:sn>${userDetails.lastName}</cas:sn>
            <cas:id>${userDetails.userId}</cas:id>
            <cas:email>${userDetails.email}</cas:email>
        </cas:attributes>
    </cas:authenticationSuccess>
</cas:serviceResponse>
        """
        stubFor(get(urlPathEqualTo("/cas/login"))
                .willReturn(aResponse()
                .withStatus(302)
                .withHeader("Location", "{{request.requestLine.query.service}}?ticket=aticket")
                .withHeader("Set-Cookie", "X-ALA-userId=\"${email}\"; Domain=ala.org.au; Path=/; HttpOnly")
                        .withBody(casXml)
                .withTransformers("response-template")))

        stubFor(get(urlPathEqualTo("/cas/p3/serviceValidate"))
            .willReturn(aResponse()
            .withStatus(200)
            .withBody(casXml)
            .withTransformers("response-template")))

        browser.go "${testConfig.security.cas.loginUrl}?service=${getConfig().baseUrl}admin"
    }

}
