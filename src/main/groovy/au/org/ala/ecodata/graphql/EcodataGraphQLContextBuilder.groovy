package au.org.ala.ecodata.graphql

import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.IdentifierHelper
import au.org.ala.ecodata.PermissionService
import au.org.ala.ecodata.Status
import au.org.ala.ecodata.UserPermission
import au.org.ala.ecodata.UserService
import grails.core.GrailsApplication
import org.grails.gorm.graphql.plugin.GraphQLContextBuilder
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired

class EcodataGraphQLContextBuilder implements GraphQLContextBuilder {

    @Autowired
    GrailsApplication grailsApplication

    @Override
    EcodataGraphQLContext buildContext(GrailsWebRequest request) {

        EcodataGraphQLContext context = new EcodataGraphQLContext()
        context.grailsApplication = grailsApplication
        context.user = UserService.currentUser()
        context.permissionService = grailsApplication.mainContext.permissionService
        context
    }


    static class EcodataGraphQLContext {
        GrailsApplication grailsApplication
        PermissionService permissionService
        def user
        Map permissionByEntityId

        void setUser(user) {
            this.user = user
            permissionByEntityId = UserPermission.findAllByUserIdAndStatusNotEqualAndAccessLevelNotEqual(userId, Status.DELETED, AccessLevel.starred).collectEntries{
                [it.entityId, it.accessLevel]
            }
        }

        String getUserId() {
            user?.userId
        }

        boolean hasPermission(Map entity) {
            if (!userId) {
                return false
            }

            boolean hasPermission = false

            // Try hub permissions first
            if (entity['hubId']) {
                List hubRoles = [AccessLevel.admin, AccessLevel.readOnly]
                hasPermission = permissionByEntityId[entity.hubId] in hubRoles
            }

            List otherRoles = [AccessLevel.admin]
            if (!hasPermission) {
                // Then see if there is a project associated ACL
                hasPermission = permissionByEntityId[entity.projectId] in otherRoles
            }
            hasPermission
        }
    }
}
