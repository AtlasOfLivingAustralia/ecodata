package au.org.ala.ecodata.graphql

import au.org.ala.ecodata.*
import grails.core.GrailsApplication
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EcodataGraphQLContextBuilder  {

    @Autowired
    GrailsApplication grailsApplication

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

        boolean hasPermission(entity) {
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
