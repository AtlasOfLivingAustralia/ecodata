package au.org.ala.ecodata

import au.org.ala.web.AlaSecured
import au.org.ala.ws.security.authenticator.AlaOidcAuthenticator
import au.org.ala.ws.security.client.AlaOidcClient
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.oauth2.sdk.token.AccessToken
import grails.converters.JSON
import grails.web.http.HttpHeaders
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.UserProfile
import org.pac4j.core.util.FindBest
import org.pac4j.jee.context.JEEContextFactory
import org.pac4j.oidc.credentials.OidcCredentials
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest
import java.text.ParseException

class ApiKeyInterceptor {

    ProjectService projectService
    ProjectActivityService projectActivityService
    UserService userService
    PermissionService permissionService
    CommonService commonService
    ActivityService activityService
    @Autowired(required = false)
    AlaOidcClient alaOidcClient
    @Autowired(required = false)
    Config config

    int order = -100 // This can go before the ala-ws-security interceptor to do the IP check

    def LOCALHOST_IP = '127.0.0.1'

    public ApiKeyInterceptor() {
        // These controllers use JWT authorization instead
        matchAll().excludes(controller: 'graphql').excludes(controller: 'paratoo').excludes(controller: 'harvest').excludes(controller:'graphqlWs')
    }

    boolean before() {
        def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
        Class controllerClass = controller?.clazz

        // The "excludes" configuration in the constructor isn't working
        if ( [ParatooController.class, HarvestController.class].contains( controllerClass ) ) {
            return true
        }

        def method = controllerClass?.getMethod(actionName?:"index", [] as Class[])
        Map result = [error: '', status : 401]

        if (controllerClass?.isAnnotationPresent(PreAuthorise) || method?.isAnnotationPresent(PreAuthorise)) {
            // What rules needs to be satisfied?
            PreAuthorise pa = method.getAnnotation(PreAuthorise) ?: controllerClass.getAnnotation(PreAuthorise)

            if (pa.basicAuth()) {
                au.org.ala.web.UserDetails user = userService.getUserFromJWT()
                request.userId = user?.userId
                if(permissionService.isUserAlaAdmin(request.userId)) {
                    /* Don't enforce check for ALA admin.*/
                }
                else if (request.userId) {
                    String accessLevel = pa.accessLevel()
                    String idType = pa.idType()
                    String entityId = params[pa.id()]

                    if (accessLevel && idType) {

                        switch (idType) {
                            case "organisationId":
                                result = permissionService.checkPermission(accessLevel, entityId, Organisation.class.name, request.userId)
                                break
                            case "projectId":
                                result = permissionService.checkPermission(accessLevel, entityId, Project.class.name, request.userId)
                                break
                            case "projectActivityId":
                                def pActivity = projectActivityService.get(entityId)
                                request.projectId = pActivity?.projectId
                                result = permissionService.checkPermission(accessLevel, pActivity?.projectId, Project.class.name, request.userId)
                                break
                            case "activityId":
                                def activity = activityService.get(entityId,'flat')
                                result = permissionService.checkPermission(accessLevel, activity?.projectId, Project.class.name, request.userId)
                                break
                            default:
                                break
                        }
                    }

                } else {
                    result.error = "Access denied"
                    result.status = 401
                }
            }

        } else {

            // Allow migration to the AlaSecured annotation.
            if (!controllerClass?.isAnnotationPresent(AlaSecured) && !method?.isAnnotationPresent(AlaSecured)) {
                // ip check
                List whiteListIP = buildWhiteList()
                List clientIp = getClientIP(request)
                boolean ipOk = checkClientIp(clientIp, whiteListIP)

                // All request without PreAuthorise annotation needs to be secured by IP for backward compatibility
                if (!ipOk) {
                    log.warn("Non-authorised IP address - ${clientIp}" )
                    result.status = 403
                    result.error = "not authorised"
                }

                // claims check
                List whiteListClientId = buildClientIdWhiteList()
                String clientId = getClientId() ?: ""
                boolean isClientIdOk = checkClientIdInWhiteList(clientId, whiteListClientId)

                // All request without PreAuthorise annotation needs to be secured by IP for backward compatibility
                if (!isClientIdOk) {
                    log.warn("Non-authorised client id - ${clientId}" )
                    result.status = 403
                    result.error = "not authorised"
                }
            }
        }

        if(result.error) {
            response.status = result.status
            render result as JSON
            return false
        }
        true
    }

    boolean after() { true }

    void afterView() { }

    /**
     * Client IP passes if it is in the whitelist of if the whitelist is empty apart from localhost.
     * @param clientIp
     * @return
     */
    boolean checkClientIp(List clientIps, List whiteList) {
        clientIps.size() > 0 && whiteList.containsAll(clientIps) || (whiteList.size() == 1 && whiteList[0] == LOCALHOST_IP)
    }

    /**
     * Client ID passes if it is in the whitelist. Fails if whitelist is empty.
     * @param clientId
     * @param whiteList
     * @return
     */
    boolean checkClientIdInWhiteList(String clientId, List whiteList) {
        whiteList.size() > 0 && clientId?.size() > 0 && whiteList.contains(clientId)
    }

    private List buildWhiteList() {
        def whiteList = [LOCALHOST_IP] // allow calls from localhost to make testing easier
        def config = grailsApplication.config.getProperty('app.api.whiteList')
        if (config) {
            whiteList.addAll(config.split(',').collect({it.trim()}))
        }
        whiteList
    }

    private List buildClientIdWhiteList() {
        def whiteList = [] // allow calls from localhost to make testing easier
        def config = grailsApplication.config.getProperty('app.clientId.whiteList')
        if (config) {
            whiteList.addAll(config.split(',').collect({it.trim()}))
        }
        whiteList
    }

    private List getClientIP(HttpServletRequest request) {
        // External requests to ecodata are proxied by Apache, which uses X-Forwarded-For to identify the original IP.
        // From grails 5, tomcat started returning duplicate headers as a comma separated list.  When a download
        // request is sent from MERIT to ecodata, ngnix adds a X-Forwarded-For header, then forwards to the
        // reporting server, which adds another header before proxying to tomcat/grails.
        List allIps = []
        Enumeration<String> ips = request.getHeaders(HttpHeaders.X_FORWARDED_FOR)
        while (ips.hasMoreElements()) {
            String ip = ips.nextElement()
            allIps.addAll(ip?.split(',').collect{it?.trim()})
        }
        allIps.add(request.getRemoteHost())
        return allIps
    }

    private String getClientId() {
        final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
        def optCredentials = alaOidcClient.getCredentials(context, config.sessionStore)
        if (optCredentials.isPresent()) {
            OidcCredentials credentials = optCredentials.get()
            return parseClientId(credentials.getAccessToken())
        }
    }

    private String parseClientId(AccessToken accessToken) {
        if (accessToken) {
            String clientIdClaim = grailsApplication.config.getProperty("app.clientId.attribute")
            final JWT jwt
            try {
                jwt = JWTParser.parse(accessToken.getValue())
            } catch (ParseException e) {
                log.error("Cannot decrypt / verify JWT")
                return null
            }

            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<SecurityContext>()
            AlaOidcAuthenticator authenticator = alaOidcClient.getAuthenticator()
            // Configure the JWT processor with a key selector to feed matching public
            // RSA keys sourced from the JWK set URL
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<SecurityContext>(authenticator.getExpectedJWSAlgs(), authenticator.getKeySource())
            jwtProcessor.setJWSKeySelector(keySelector)

            // Set the required JWT claims for access tokens issued by the server
            jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier(new JWTClaimsSet.Builder().issuer(authenticator.issuer.getValue()).build(), Set.copyOf(authenticator.requiredClaims)))

            try {
                JWTClaimsSet claimsSet = jwtProcessor.process(jwt, null)
                return (String) claimsSet.getClaim(clientIdClaim)
            }
            catch (Exception e) {
                log.error("Cannot decrypt / verify JWT")
            }
        }
    }

}
