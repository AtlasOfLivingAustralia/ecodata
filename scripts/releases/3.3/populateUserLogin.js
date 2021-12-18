// Populates the last user login time for MERIT users.
// Note this script should be run after:
// 1) addHubIdsToEntities AND
// 2) the migration of the CAS roles into ecodata.
var permissions = db.userPermission.find({status:{$ne:'deleted'}});
var now = ISODate(); // We are just going to start everyone at the time this is implemented.
var users = {};

var meritHubId = db.hub.findOne({urlPath:'merit'}).hubId;
while (permissions.hasNext()) {

    var isMERIT = false;
    var permission = permissions.next();
    if (!users[permission.userId]) {

        switch (permission.entityType) {
            case 'au.org.ala.ecodata.Project':
                isMERIT = db.project.count({projectId:permission.entityId, hubId:meritHubId}) > 0;
                break;
            case 'au.org.ala.ecodata.Hub':
                isMERIT = permission.entityId == meritHubId;
                break;
            case 'au.org.ala.ecodata.Organisation':
                isMERIT = db.organisation.count({organisation:permission.entityId, hubId:meritHubId}) > 0;
                break;
            case 'au.org.ala.ecodata.ManagementUnit':
                isMERIT = db.managementUnit.count({managementUnitId:permission.entityId, hubId:meritHubId}) > 0;
                break;
            case 'au.org.ala.ecodata.Program':
                isMERIT = db.program.count({programId:permission.entityId, hubId:meritHubId}) > 0;
                break;
        }
        if (isMERIT) {
            var user = {
                userId:permission.userId,
                dateCreated:now,
                lastUpdated:now,
                status:'active',
                userHubs: [
                    {
                        hubId:meritHubId,
                        lastLoginTime:now
                    }
                ]
            };
            db.user.insert(user);
            users[permission.userId] = user;
        }
    }
}