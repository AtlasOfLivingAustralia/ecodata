package au.org.ala.ecodata.graphql

import au.org.ala.ecodata.PermissionService
import au.org.ala.ecodata.UserService
import grails.core.GrailsApplication
import org.grails.gorm.graphql.plugin.DefaultGraphQLContextBuilder
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired

class EcodataGraphQLContextBuilder extends DefaultGraphQLContextBuilder {

    @Autowired
    GrailsApplication grailsApplication

    @Override
    Map buildContext(GrailsWebRequest request) {
        Map context = super.buildContext(request)

        context.grailsApplication = grailsApplication
        context.userService = grailsApplication.mainContext.userService
        context.user = UserService.currentUser()

        context.permissionService = grailsApplication.mainContext.permissionService
        context
    }
}
