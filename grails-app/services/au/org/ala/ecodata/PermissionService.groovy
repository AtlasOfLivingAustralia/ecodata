package au.org.ala.ecodata

import au.org.ala.web.AuthService
import grails.gorm.DetachedCriteria
import org.grails.datastore.mapping.query.api.BuildableCriteria

import static au.org.ala.ecodata.Status.DELETED
/**
 * Service to set and get permissions on projects for each user
 */
class PermissionService {

    static transactional = false
    AuthService authService
    UserService userService
    ProjectController projectController
    def grailsApplication, webService, hubService

    /** Limit to the maximum number of UserPermissions returned by queries */
    static final int MAX_QUERY_RESULT_SIZE = 1000

    boolean isUserAlaAdmin(String userId) {
        userId && userService.getRolesForUser(userId)?.contains("ROLE_ADMIN")
    }

    public boolean isUserAdminForProject(String userId, String projectId) {
        def isAdmin = false

        if (userId && projectId) {
            isAdmin = ( UserPermission.findAllByUserIdAndEntityTypeAndEntityIdAndAccessLevel(userId, Project.class.name, projectId, AccessLevel.admin) )
        }

        return isAdmin
    }

    public boolean isUserAdminForOrganisation(String userId, String organisationId) {
        def isAdmin = false

        if (userId && organisationId) {
            isAdmin = ( UserPermission.findAllByUserIdAndEntityTypeAndEntityIdAndAccessLevel(userId, Organisation.class.name, organisationId, AccessLevel.admin) )
        }

        return isAdmin
    }

    def isUserEditorForProject(String userId, String projectId) {
        def isEditor = false

        if (userId && projectId) {
            def ups = getUserAccessForEntity(userId, Project, projectId)
            ups.findAll {
                if (it.accessLevel.code >= AccessLevel.editor.code) {
                    isEditor = true
                }
            }
        }

        return isEditor // bolean
    }

    def isUserEditorForProjects(String userId, String projectIds) {
        Boolean userHasPermission = false

        if (userId && projectIds) {
            userHasPermission = true
            List ids = projectIds.split(',')
            ids.each { String projectId ->
                def ups = getUserAccessForEntity(userId, Project, projectId)
                ups = ups.findAll {
                    it.accessLevel.code >= AccessLevel.editor.code
                }

                userHasPermission &= !!ups
            }
        }

        log.debug "userHasPermission = ${userHasPermission}"
        return userHasPermission // bolean
    }

    Boolean canUserModerateProjects(String userId, String projectIds) {
        Boolean userHasPermission = false

        if (userId && projectIds) {
            userHasPermission = true
            List ids = projectIds.split(',')
            ids.each { String projectId ->
                def ups = getUserAccessForEntity(userId, Project, projectId)
                ups = ups.findAll {
                    it.accessLevel.code >= AccessLevel.moderator.code
                }

                userHasPermission &= !!ups
            }
        }

        log.debug "userHasPermission = ${userHasPermission}"
        return userHasPermission // bolean
    }

    /**
     * Given a userId and a list of projects, check if the user has edit permission on each project.
     * The function returns a map with projectId as key and boolean for permission. Null if project is not found.
     * @param userId
     * @param projectIds []
     * @return
     */
    Map isUserAdminForProjects(String userId, List projectIds) {
        Map permissions =[:]
        Boolean isEditor = false
        Project project

        if (userId && projectIds) {
            projectIds.each { projectId ->
                project = Project.findByProjectId(projectId)
                if(project){
                    permissions[projectId] = isUserAdminForProject(userId, projectId);
                } else {
                    permissions[projectId] = null;
                }
            }
        } else if(userId == ''){
            // for anonymous user
            projectIds.each {
                permissions[it] = false;
            }
        }

        return permissions
    }

    def isUserGrantManagerForOrganisation(String userId, String organisationId) {
        def isGrantManager = false
        if (userId && organisationId) {
            def ups = getUserAccessForEntity(userId, Organisation, organisationId)
            ups.findAll {
                if (it.accessLevel.code >= AccessLevel.caseManager.code) {
                    isGrantManager = true
                }
            }
        }

        return isGrantManager
    }

    /**
     *
     * @param projectId
     * @param roles
     * @return
     */
    boolean isUserMemberOfProject(String userId, String projectId, List roles = [AccessLevel.admin, AccessLevel.caseManager, AccessLevel.moderator, AccessLevel.editor, AccessLevel.projectParticipant]) {
        def up = UserPermission.findByEntityIdAndEntityTypeAndAccessLevelNotEqualAndAccessLevelInListAndUserId(projectId, Project.class.name, AccessLevel.starred, roles, userId)
        up != null
    }

    private def getUserAccessForEntity(String userId, Class entityType, String entityId ) {
        return UserPermission.findAllByUserIdAndEntityTypeAndEntityId(userId, entityType.name, entityId)

    }

    def addUserAsEditorToProject(currentUserId, targetUserId, projectId) {
        if ((isUserAdminForProject(currentUserId, projectId) || authService.userInRole("ROLE_ADMIN")) && targetUserId) {
            addUserAsRoleToProject(targetUserId, AccessLevel.editor, projectId)
        }
    }

    def addUserAsAdminToProject(String userId, String projectId) {
        addUserAsRoleToProject(userId, AccessLevel.admin, projectId)
    }

    def getUsersForProject(String projectId) {
        def up = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevel(projectId, Project.class.name, AccessLevel.editor)
        up.collect { it.userId } // return just a list of userIds
    }

    def getUsersForSite(String siteId) {
        def up = UserPermission.findAllByEntityIdAndEntityType(siteId, Site.class.name)
        up.collect { it.userId } // return just a list of userIds
    }

    List<String> getProjectsForUser(String userId, AccessLevel... accessLevels) {
        UserPermission.withCriteria {
            eq "userId", userId
            eq "entityType", Project.class.name
            ne "status", DELETED

            if (accessLevels) {
                'in' "accessLevel", accessLevels
            }

            projections {
                property("entityId")
            }
        }
    }
    /*
        Bulk load members of Project to improve loading perforamnce of a project which has large number of memebers
     */
    def getMembersForProject(String projectId, List roles = [AccessLevel.admin, AccessLevel.caseManager, AccessLevel.moderator, AccessLevel.editor, AccessLevel.projectParticipant]) {
        def up = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevelNotEqualAndAccessLevelInList(projectId, Project.class.name, AccessLevel.starred, roles)
        Map out = [:]
        List userIds = []
        up.each{
            userIds.add(it.userId)
            Map rec=[:]
            rec.userId = it.userId
            rec.role = it.accessLevel?.toString()
            out.put(it.userId,rec);

        }
        def userList = authService.getUserDetailsById(userIds)

        if (userList) {
            def users = userList['users']

            users.each { k, v ->
                Map rec = out.get(k)
                if (rec) {
                    rec.displayName = v?.displayName
                    rec.userName = v?.userName
                }
            }
        }
        out.values().toList();

    }

    /**
     * Return project members, support pagination
     * @param projectId Project Id
     * @param offset Page starting position
     * @param max Page size
     * @param roles Member roles
     * @return One page of project member details
     */
    def getMembersForProjectPerPage(String projectId, Integer offset, Integer max, List roles = [AccessLevel.admin, AccessLevel.caseManager, AccessLevel.moderator, AccessLevel.editor, AccessLevel.projectParticipant]) {
        List admins = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevelNotEqualAndAccessLevel(projectId, Project.class.name, AccessLevel.starred, AccessLevel.admin)

        BuildableCriteria criteria = UserPermission.createCriteria()
        List memebers = criteria.list(max:max, offset:offset) {
            eq("entityId", projectId)
            eq("entityType", Project.class.name)
            ne("accessLevel", AccessLevel.starred)
            inList("accessLevel", roles)
        }

        Map out = [:]
        List userIds = []
        memebers.each{
            userIds.add(it.userId)
            Map rec=[:]
            rec.userId = it.userId
            rec.role = it.accessLevel?.toString()
            out.put(it.userId,rec)

        }

        def userList = authService.getUserDetailsById(userIds)
        if (userList) {
            def users = userList['users']

            users.each { k, v ->
                Map rec = out.get(k)
                if (rec) {
                    rec.displayName = v?.displayName
                    rec.userName = v?.userName
                }
            }
        }
        [totalNbrOfAdmins: admins.size(), data:out.values(), count:memebers.totalCount]
    }

    def getMembersForOrganisation(String organisationId) {
        def up = UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevelNotEqual(organisationId, Organisation.class.name, AccessLevel.starred)
        def out = []
        up.each {
            def rec = [:]
            def u = userService.getUserForUserId(it.userId?:"0")
            rec.role = it.accessLevel?.toString()
            rec.userId = it.userId
            rec.displayName = u?.displayName
            rec.userName = u?.userName
            out.add(rec)
        }
        out
    }

    /**
     * Returns a list of all users who have permissions configured for the specified hub.
     * @param hubId the hubId of the hub to get permissions for.
     * @param includeUserDetails if true, lookup the userId in the UserDetails application to get the user name,
     * roles etc.
     * @return a List of the users that have roles configured for the hub.
     */
    List<Map> getMembersForHub(String hubId, boolean includeUserDetails = true) {
        List permissions = UserPermission.findAllByEntityIdAndEntityTypeAndStatusNotEqual(hubId, Hub.class.name, DELETED)
        permissions.collect{toMap(it, includeUserDetails)}
    }

    /**
     * Return Hub members, support pagination
     * @param hubId The hubId of the Hub that was logged into
     * @param offset Page starting position
     * @param max Page size
     * @param roles List of Hub roles that will be included in the criteria
     * @return Hub members one page at a time
     */
    def getMembersForHubPerPage(String hubId, Integer offset, Integer max, String userId, List roles = [AccessLevel.admin, AccessLevel.caseManager, AccessLevel.readOnly]) {
        BuildableCriteria criteria = UserPermission.createCriteria()
        List members = criteria.list(max:max, offset:offset) {
            if (userId && userId != "null") {
                eq("userId", userId)
            }
            eq("entityId", hubId)
            eq("entityType", Hub.class.name)
            ne("accessLevel", AccessLevel.starred)
            inList("accessLevel", roles)
            order("accessLevel", "asc")


        }

        Map out = [:]
        List userIds = []
        members.each {
            userIds.add(it.userId)
            out.put(it.userId,toMap(it,false))
        }

        def userList = authService.getUserDetailsById(userIds)
        if (userList) {
            def users = userList['users']

            users.each { k, v ->
                Map rec = out.get(k)
                if (rec) {
                    rec.displayName = v?.displayName
                    rec.userName = v?.userName
                }
            }
        }

        [data:out.values(), count:members.totalCount]

    }

    /**
     * Returns a list of all users who have permissions configured for the specified program.
     * @param programId the programId of the program to get permissions for.
     * @return a List of the users that have roles configured for the program.
     */
    Map getMembersOfProgram(String programId, Integer max = 100, Integer offset = 0, String order = "asc", String sort = "accessLevel") {
        List permissions = UserPermission.findAllByEntityIdAndEntityTypeAndStatusNotEqual(
                programId, Program.name, DELETED, [max:max, offset:offset, sort:sort, order:order])
        List members = permissions.collect{toMap(it)}
        [programId:programId, members:members]
    }

    /**
     * Returns a list of all users who have permissions configured for the specified ManagementUnit.
     * @param managementUnitId the programId of the ManagementUnit to get permissions for.
     * @return a List of the users that have roles configured for the ManagementUnit.
     */
    Map getMembersOfManagementUnit(String managementUnitId, Integer max = 100, Integer offset = 0, String order = "asc", String sort = "accessLevel") {
        List permissions = UserPermission.findAllByEntityIdAndEntityTypeAndStatusNotEqual(
                managementUnitId, ManagementUnit.name, DELETED, [max:max, offset:offset, sort:sort, order:order])
        List members = permissions.collect{toMap(it)}
        [managementUnitId:managementUnitId, members:members]
    }

    /**
     * Converts a UserPermission into a Map, looking up the user display name from the user details service
     * if requested.
     */
    private Map toMap(UserPermission userPermission, boolean includeUserDetails = true) {
        Map mapped = [:]
        mapped.role = userPermission.accessLevel?.toString()
        mapped.userId = userPermission.userId
        if (userPermission.expiryDate) {
            mapped.expiryDate = userPermission.expiryDate
        }

        if (includeUserDetails) {
            def u = userService.getUserForUserId(userPermission.userId)
            mapped.displayName = u?.displayName
            mapped.userName = u?.userName
        }
        mapped
    }

    private def addUserAsRoleToEntity(String userId, AccessLevel accessLevel, Class entityType, String entityId) {
        List prevRoles = UserPermission.findAllByUserIdAndEntityIdAndEntityTypeAndAccessLevelNotEqual(userId, entityId, entityType.name, AccessLevel.starred)
        log.debug "0. prevRoles = ${prevRoles}"

        // It's possible that a user could attempt to reassign permission at the same level they already have
        UserPermission up = prevRoles.find{it.accessLevel == accessLevel}
        if (!up) {
            try {
                up = new UserPermission(userId: userId, entityId: entityId, entityType: entityType.name, accessLevel: accessLevel)
                up.save(flush: true, failOnError: true)

            } catch (Exception e) {
                def msg = "Failed to save UserPermission: ${e.message}"
                log.error msg, e
                return [status: 'error', error: msg]
            }
        }
        if (accessLevel != AccessLevel.starred) {
            // remove any lower roles
            prevRoles.each {
                log.debug "1. prevRole = ${it}"
                if (it != up) {
                    try {
                        it.delete(flush: true)
                        //return [status:'ok', id: it.id]
                    } catch (Exception e) {
                        def msg = "Failed to delete (previous) UserPermission: ${e.message}"
                        log.error msg, e
                        return [status:'error', error: msg]
                    }
                }

            }
        }
        return [status:'ok', id: up.id]
    }

    def addUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        return addUserAsRoleToEntity(userId, accessLevel, Project, projectId)
    }

    def addUserAsRoleToSite(String userId, AccessLevel accessLevel, String siteId) {
        return addUserAsRoleToEntity(userId, accessLevel, Site, siteId)
    }

    def removeUserAsRoleToSite(String userId, AccessLevel accessLevel, String siteId) {
        removeUserAsRoleToEntity(userId, accessLevel, Site, siteId)
    }

    def addUserAsRoleToOrganisation(String userId, AccessLevel accessLevel, String organisationId) {
        return addUserAsRoleToEntity(userId, accessLevel, Organisation, organisationId)
    }

    private Map removeUserAsRoleToEntity(String userId, AccessLevel accessLevel, Class entityType, String entityId) {
        def up = UserPermission.findByUserIdAndEntityIdAndEntityTypeAndAccessLevel(userId, entityId, entityType.name, accessLevel)
        if (up) {
            try {
                up.delete(flush: true)
                return [status:'ok', id: up.id]
            } catch (Exception e) {
                def msg = "Failed to delete UserPermission: ${e.message}"
                log.error msg, e
                return [status:'error', error: msg]
            }
        }
        return [status:'ok']
    }

    def removeUserAsRoleToProject(String userId, AccessLevel accessLevel, String projectId) {
        removeUserAsRoleToEntity(userId, accessLevel, Project, projectId)
    }

    def removeUserAsRoleFromOrganisation(String userId, AccessLevel accessLevel, String organisationId) {
        removeUserAsRoleToEntity(userId, accessLevel, Organisation, organisationId)
    }
    /**
     * Deletes all permissions associated with the supplied project.  Used as a part of a project delete operation.
     *
     * Permissions can be soft deleted (by setting status = deleted), or hard deleted (if the 'destroy' parameter is true)
     *
     * @param projectId The id of the project whose permissions are being deleted. Mandatory.
     * @param destroy True to physically delete the permissions, false to soft delete. Optional - defaults to false.
     */
    void deleteAllForProject(String projectId, boolean destroy = false) {
        UserPermission.findAllByEntityId(projectId).each {
            if (destroy) {
                it.delete()
            } else {
                it.status = 'deleted'
                it.save(flush: true)
            }
        }
    }

    List getAllUserPermissionForEntity(String id, String type, String accessLevel = null){
        accessLevel ?
                UserPermission.findAllByEntityIdAndEntityTypeAndAccessLevel(id, type, accessLevel) :
                UserPermission.findAllByEntityIdAndEntityType(id, type)

    }

    List getAllAdminsForProject(String id){
        getAllUserPermissionForEntity(id, Project.class.name, 'admin')
    }

    Map addUserAsRoleToProgram(String userId, AccessLevel accessLevel, String programId) {
        return addUserAsRoleToEntity(userId, accessLevel, Program, programId)
    }

    Map removeUserAsRoleFromProgram(String userId, AccessLevel accessLevel, String programId) {
        return removeUserAsRoleToEntity(userId, accessLevel, Program, programId)
    }

    Map addUserAsRoleToManagementUnit(String userId, AccessLevel accessLevel, String managementUnitId) {
        return addUserAsRoleToEntity(userId, accessLevel, ManagementUnit, managementUnitId)
    }

    Map removeUserAsRoleFromManagementUnit(String userId, AccessLevel accessLevel, String managementUnitId) {
        return removeUserAsRoleToEntity(userId, accessLevel, ManagementUnit, managementUnitId)
    }

    Map addUserAsRoleToHub(Map params) {
        return saveUserToHubEntity(params)
    }

    Map removeUserRoleFromHub(Map params) {
        return removeUserAsRoleToEntity(params.userId,AccessLevel.valueOf(params.role),Hub,params.entityId)
    }

    /**
     * Check user permission against given access code
     *
     * @param userId
     * @param entityId
     * @param entityType
     * @param accessCode
     */
    boolean checkUserPermission(String userId, String entityId, String entityType, int accessCode) {
        def userPermission = UserPermission.findByUserIdAndEntityIdAndEntityType(userId, entityId, entityType)
        userPermission ? userPermission.accessLevel.code >= accessCode  : false
    }

    /**
     * Check user permission against given accessLevel
     *
     * @param accessLevel
     * @param entityId
     * @param entityType
     * @param userId
     */
    Map checkPermission(String accessLevel, String entityId, String entityType, String userId){
        Map result = [error:'', status: 401]
        switch (accessLevel) {
            case AccessLevel.admin.name():
                if(!checkUserPermission(userId, entityId, entityType, AccessLevel.admin.code)){
                    result.error = "Access denied, user does not have admin permission for the project."
                }
                break
            case AccessLevel.caseManager.name():
                if(!checkUserPermission(userId, entityId, entityType, AccessLevel.caseManager.code)) {
                    result.error = "Access denied, user does not have caseManager permission for the project."
                }
                break
            case AccessLevel.editor.name():
                if(!checkUserPermission(userId, entityId, entityType, AccessLevel.editor.code)) {
                    result.error = "Access denied, user does not have editor permission for the project."
                }
                break
            case AccessLevel.projectParticipant.name():
                if(!checkUserPermission(userId, entityId, entityType, AccessLevel.projectParticipant.code)){
                    result.error = "Access denied, user does not have projectParticipant permission for the project."
                }
                break

            default:
                log.warn "Unexpected role: ${accessLevel}"
                break
        }

        result
    }

    Map deleteUserPermissionByUserId(String userId, String hubId){
        log.info("Deleting all permissions for user: "+userId+ " related to hub: "+hubId)
        List<UserPermission> permissions = UserPermission.findAllByUserId(userId)
        if (permissions.size() > 0) {
            permissions.each {
                boolean isInHub = isEntityOwnedByHub(it.entityId, it.entityType, hubId)
                if (isInHub){
                    try {
                        it.delete(flush: true, failOnError: true)
                        if (log.isDebugEnabled()) {
                            log.debug("Removed permission for entity: "+it.entityId +" for user: " + userId)
                        }

                    } catch (Exception e) {
                        String msg = "Failed to delete UserPermission: ${e.message}"
                        log.error msg, e
                        return [status: 500, error: msg]
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Not removing permission for entity "+it.entityId+" as it is not associated with the hub")
                    }
                }

            }
            return [status: 200, error: false]

        } else {
            return [status: 400, error: "No User Permissions found"]
        }

    }

    /**
     * Returns a list of permissions that have an expiry date less than or equal to the
     * supplied date
     */
    List<UserPermission> findPermissionsByExpiryDate(Date date = new Date()) {
        UserPermission.findAllByExpiryDateLessThanEqualsAndStatusNotEqual(date, DELETED)
    }

    /**
     *  Checks to see if an entity has a matching hubId to the supplied hubId.
     *  Organisations are a special case - they also check if the organisation is running
     *  any MERIT projects in which case true will be returned.
     * @param entityId The id (programId/projectId etc) of the entity to check
     * @param entityType The type of entity to check (class.getName())
     * @param hubId the hubId to check against
     * @return true if the entity is owned by the supplied hub
     */
    private boolean isEntityOwnedByHub(String entityId, String entityType, String hubId) {
        int count = 0
        if (entityType == Organisation.class.name) {
            count = Organisation.countByOrganisationIdAndHubId(entityId, hubId)
            if (count == 0) {
                DetachedCriteria query = Project.where {
                    (organisationId == entityId || orgIdSvcProvider == entityId) && hubId == hubId
                }
                count = query.count()
            }
        } else if (entityType == Program.class.name) {
            count = Program.countByProgramIdAndHubId(entityId, hubId)
        } else if (entityType == Project.class.name) {
            count = Project.countByProjectIdAndHubId(entityId, hubId)
        } else if (entityType == ManagementUnit.class.name) {
            count = ManagementUnit.countByManagementUnitIdAndHubId(entityId, hubId)
        } else if (entityType == Hub.class.name) {
            count = Hub.countByHubId(entityId, hubId)
        }
        return count > 0
    }

    /**
     * This method finds the hubId of the entity specified in the supplied UserPermission.
     * Currently only Project, Organisation, ManagementUnit, Program are supported.
     */
    String findOwningHubId(UserPermission permission) {
        if (!(permission.entityType in [Project.class.name, Organisation.class.name, ManagementUnit.class.name, Program.class.name, Hub.class.name])) {
            throw new IllegalArgumentException("Permissions with entityType = $permission.entityType are not supported")
        }
        Class entity = Class.forName(permission.entityType)
        String propertyName = IdentifierHelper.getEntityIdPropertyName(permission.entityType)
        String hubId = new DetachedCriteria(entity).get {
            eq(propertyName, permission.entityId)
            projections {
                property('hubId')
            }
        }
        hubId
    }

    private Map saveUserToHubEntity(Map params) {
        UserPermission up = UserPermission.findByUserIdAndEntityIdAndEntityType(params.userId, params.entityId, Hub.name)
        try {
            if (up) {
                if (params.expiryDate) {
                    up.expiryDate = DateUtil.parse(params.expiryDate)
                } else {
                    up.expiryDate = null
                }

                up.accessLevel = AccessLevel.valueOf(params.role) ?: up.accessLevel
                up.save(flush: true, failOnError: true)
            } else {
                Date expiration = null
                if (params.expiryDate) {
                    expiration = DateUtil.parse(params.expiryDate)
                }

                up = new UserPermission(userId: params.userId, entityId: params.entityId, entityType: Hub.name, accessLevel: AccessLevel.valueOf(params.role), expiryDate:expiration)
                up.save(flush: true, failOnError: true)
            }
        } catch (Exception e) {
            def msg = "Failed to save UserPermission: ${e.message}"
            log.error msg, e
            return [status: 'error', error: msg]
        }

        return [status:'ok', id: up.id]
    }

    /**
     * This method returns the UserPermission details
     */
    UserPermission findUserPermission(String userId, String hubId) {
        UserPermission.findByUserIdAndEntityIdAndStatusNotEqual(userId, hubId, DELETED)
    }

    /**
     * Returns a list of permissions that have an expiry date greater than or equal to the
     * supplied date
     */
    List<UserPermission> findAllByExpiryDate(Date fromDate, Date toDate) {
        List permissions
        if (!fromDate) {
            permissions = UserPermission.findAllByExpiryDateLessThanAndStatusNotEqual(toDate, DELETED)
        }
        else if (!toDate) {
            permissions = UserPermission.findAllByExpiryDateGreaterThanEqualsAndStatusNotEqual(fromDate, DELETED)
        }
        else {
            permissions = UserPermission.findAllByExpiryDateBetweenAndStatusNotEqual(fromDate, toDate, DELETED)
        }
        permissions
    }

    /**
     * Given a UserPermission, finds the user permission for the owning hub of the entity described in the permission.
     */
    UserPermission findParentPermission(UserPermission userPermission) {
        String hubId = findOwningHubId(userPermission)
        UserPermission parentPermission = null
        if (hubId) {
            parentPermission = UserPermission.findByUserIdAndEntityIdAndEntityTypeAndStatusNotEqual(userPermission.userId, hubId, Hub.name, DELETED)
        }
        parentPermission
    }

}
