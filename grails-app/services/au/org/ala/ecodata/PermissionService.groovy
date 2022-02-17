package au.org.ala.ecodata

import au.org.ala.web.AuthService
import au.org.ala.web.CASRoles
import grails.gorm.DetachedCriteria
import org.grails.datastore.mapping.query.api.BuildableCriteria

import static au.org.ala.ecodata.Status.DELETED
/**
 * Service to set and get permissions on projects for each user
 */
class PermissionService {

    static transactional = false
    AuthService authService
    UserService userService // found in ala-auth-plugin
    ProjectController projectController
    def grailsApplication, webService, hubService

    /** Limit to the maximum number of UserPermissions returned by queries */
    static final int MAX_QUERY_RESULT_SIZE = 1000

    boolean isUserAlaAdmin(String userId) {
        userId && userService.getRolesForUser(userId)?.contains(CASRoles.ROLE_ADMIN)
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
        List<UserPermission> permissions = UserPermission.findAllByUserId(userId)
        if (permissions.size() > 0) {
            permissions.each {
                def isInHub = isEntityOwnedByHub(it.entityId, it.entityType, hubId)
                if (isInHub){
                    try {
                        it.delete(flush: true, failOnError: true)
                        log.info("The Permission is removed for this user: " + userId)
                    } catch (Exception e) {
                        String msg = "Failed to delete UserPermission: ${e.message}"
                        log.error msg, e
                        return [status: 500, error: msg]
                    }
                }else{
                    log.info("This entity Id is not a merit : " + it.entityId)
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
        }
        return count > 0
    }

    /**
     * This code snippet is based on ReportService.userSummary
     * Produces a list of users containing roles below:
     * (ROLE_FC_READ_ONLY,ROLE_FC_OFFICER,ROLE_FC_ADMIN)
     */
    private def extractUserDetails() {
        List roles = ['ROLE_FC_READ_ONLY', 'ROLE_FC_OFFICER', 'ROLE_FC_ADMIN']
        def userDetailsSummary = [:]

        int batchSize = 500

        String url = grailsApplication.config.userDetails.admin.url
        url += "/userRole/list?format=json&max=${batchSize}&role="
        roles.each { role ->
            int offset = 0
            Map result = webService.getJson(url+role+'&offset='+offset)

            while (offset < result?.count && !result?.error) {

                List usersForRole = result?.users ?: []
                usersForRole.each { user ->
                    if (userDetailsSummary[user.userId]) {
                        userDetailsSummary[user.userId].role = role
                    }
                    else {
                        user.projects = []
                        user.name = (user.firstName ?: "" + " " +user.lastName ?: "").trim()
                        user.role = role
                        userDetailsSummary[user.userId] = user
                    }


                }

                offset += batchSize
                result = webService.getJson(url+role+'&offset='+offset)
            }

            if (!result || result.error) {
                log.error("Error getting user details for role: "+role)
                return
            }
        }

        userDetailsSummary
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

    def saveUserDetails() {
        def map = [ROLE_FC_ADMIN: "admin", ROLE_FC_OFFICER: "caseManager", ROLE_FC_READ_ONLY: "readOnly"]
        String urlPath = "merit"
        String hubId = hubService.findByUrlPath(urlPath)?.hubId

        //extracts from UserDetails
        def userDetailsSummary = extractUserDetails()

        //save to userPermission
        userDetailsSummary.each { key, value ->
            value.roles.each { role ->
                if (map[role]) {
                    UserPermission userP = UserPermission.findByUserIdAndEntityIdAndEntityType(key, hubId, Hub.name)
                    try {
                        if (!userP) {
                            UserPermission up = new UserPermission(userId: key, entityId: hubId, entityType: Hub.name, accessLevel: AccessLevel.valueOf(map[role]))
                            up.save(flush: true, failOnError: true)
                        }
                    } catch (Exception e) {
                        def msg = "Failed to save UserPermission: ${e.message}"
                        return [status: 'error', error: msg]
                    }
                }

            }
        }
    }

    // Permission:
    // create / read / update / delete
    // read_children / update_children / delete_children?
    // query_children /
    // view_reports?  (this is specific to QLD hubs, might be useful for DoEE)

    // schema options: (non-relational)
    // entityId / entityType? / owner (dbRef)
    // embeddedCollection: [userId, [permissions]]

    // More relationalish? - might be better as embedded collection will get very big for some projects (e.g. sightings)
    // EntityId, entity type, owner. _id (is this optional?), public?, viewableInOtherHubs...?, etc
    // keep existing.
    // Role / permission mapping. (for convenience of role / permission assignment)
    boolean checkUserPermission(String userId, String entityId, String entityType, String permission) {
        UserPermission userPermission = UserPermission.findByUserIdAndEntityIdAndEntityType(userId, entityId, entityType)

        // How many queries... 1. To get UserPermission 2. To see if public (if read) 3. To get owner...  Fair bit of overhead, could add an extra table to contain owner/visiblity which would reduce this to 2?


        // Need to check if the entity is "public" - maybe we could have a user permission entry for "all_users" or something for this?
        // Does this only apply to documents?  Or do we need it on sites etc?  (probably, yes so project areas can be public?)

//        if (permission == 'read' /*&& entity.getVisibility == public*/) {
//            return true
//        }

        boolean hasPermission = false
        if (userPermission && userPermission.hasPermission(permission)) {
            hasPermission = true
        }
        else {

            Owner owner = findOwner(entityId, entityType)  // Do we do an extra query here or update the permission table to directly include an owner reference?

            if (owner) {
                // Does "read" on an owner automatically give access to children or do we need "read_children"?
                // Same for "create" / "update" / "delete"

                // Recursively go up the permission tree
                hasPermission = checkUserPermission(userId, owner.entityId, owner.entityType, permission)
            }
        }
        hasPermission
    }
    class Owner {
        Owner(String entityId, String entityType) {
            this.entityId = entityId
            this.entityType = entityType
        }
        String entityType
        String entityId
    }

    interface HasOwner {
        Owner getOwner()

    }

    class OwnerFinder {

        Owner getOwner(Project project) {
            if (project.hubId) {
                new Owner(project.hubId, Hub.name)
            }
        }
        Owner getOwner(Activity activity) {
            if (activity.projectId) {
                new Owner(activity.projectId, Project.name)
            }
            else if (activity.projectActivityId) {
                new Owner(activity.projectActivityId, ProjectActivity.name)
            }
            null
        }
        Owner getOwner(Organisation organisation) {
            null
        }
        Owner getOwner(Hub hub) {
            null
        }
        Owner getOwner(Output output) {
            new Owner(output.activityId, Activity.name)
        }
        Owner getOwner(ManagementUnit managementUnit) {
            null
        }
        Owner getOwner(ProjectActivity projectActivity) {
            new Owner(projectActivity.projectId, Project.name)
        }
        Owner getOwner(Report report) {
            if (report.projectId) {
                return new Owner(report.projectId, Project.name)
            }
            else if (report.managementUnitId) {
                return new Owner(report.managementUnitId, ManagementUnit.name)
            }
            else if (report.organisationId) {
                return new Owner(report.organisationId, Organisation.name)
            }
            null
        }

        Owner getOwner(Document document) {
            if (document.projectId) {
                return new Owner(document.projectId, Project.name)
            }
            else if (document.activityId) {
                return new Owner(document.activityId, Activity.name)
            }
            else if (document.outputId) {
                return new Owner(document.outputId, Output.name)
            }
            else if (document.managementUnitId) {
                return new Owner(document.managementUnitId, ManagementUnit.name)
            }
            else if (document.programId) {
                return new Owner(document.programId, ManagementUnit.name)
            }
            else if (document.organisationId) {
                return new Owner(document.organisationId)
            }
            else if (document.projectActivityId) {
                return new Owner(document.projectActivityId, ProjectActivity.name)
            }
        }

    }

    private Owner findOwner(String entityId, String entityType) {
        // Three options.
        // 1. Denormalise UserPermission and have an owner reference.
        // 2. Have extra table with this information.
        // 3. Include a method for finding the owner based on the entity.

        // getOwner method in domain object itself. (could just return id & type to avoid extra query)
        // entity with owner
        Object entity = IdentifierHelper.load(entityId, entityType)
        new OwnerFinder().getOwner(entity)


    }

    // e.g for document
    //@Permission(read, ..)
    def annotationCheck() {
        // get entity id, get permission, get entity type?

        // query user permission directly.
        // if exists continue.

        // if not exists query entity record
        // if permission is read and entity is public all good.

        // query permissions for user based on owner record, transform permission to "${permission}_children" e.g. read_children
        // or is this implicit?


        // how does role / permission mapping work?  extra level of indirection?

        // Role table, with permissions per role.  do we allow a user to have both a role and an extra permission?  or does that make user admin too complex?


        // userId, entity id, role, permissions...?

        checkPermission()


    }

    /** Global permission check, used for API permission.  Could also use a hub based check? */
    boolean checkUserPermission(String userId, String permission) {
        checkUserPermission(userId, "api", "api", permission)
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
    List<UserPermission> findAllByExpiryDate(Date date = new Date()) {
        UserPermission.findAllByExpiryDateGreaterThanEqualsAndStatusNotEqual(date, DELETED)
    }

}
