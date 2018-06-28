var users = ['43447'];
var adminUserId = '1493';
for (var i=0; i<users.length; i++) {
    var permissions = db.userPermission.find({userId:users[i], status:{$ne:'deleted'}});
    while (permissions.hasNext()) {

       var permission = permissions.next();

       var auditMessage = {
            date:ISODate(),
            entity:permission,
            eventType:'Delete',
            entityType:'au.org.ala.ecodata.UserPermission',
            entityId:permission._id,
            userId:adminUserId
        };
        if (permission.entityType == 'au.org.ala.ecodata.Project') {
            auditMessage.projectId = permission.entityId;
        }

        db.auditMessage.insert(auditMessage);
        db.userPermission.remove({_id:permission._id});
    }
}