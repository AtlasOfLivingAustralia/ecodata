var userIds = ['1493'];

var rlp = db.program.find({name:'Regional Land Partnerships'}).next();
var mus = db.managementUnit.find({status:{$ne:'deleted'}});
while (mus.hasNext()) {
    var mu = mus.next();
    print('"'+mu.name+'",'+'https://fieldcapture.ala.org.au/rlp/index/'+mu.programId);

    for (var i=0; i<userIds.length; i++) {
        var permissionExists = db.userPermissions.find({entityId:mu.managementUnitId, userId:userIds[i]});
        if (!permissionExists.hasNext()) {
            db.userPermission.insert(
                {
                    entityId:mu.managementUnitId,
                    entityType:'au.org.ala.ecodata.ManagementUnit',
                    userId:userIds[i],
                    accessLevel:'caseManager',
                    status:'active'}
            );
        }

        var projects = db.project.find({managementUnitId:mu.managementUnitId});

        while (projects.hasNext()) {
            var project = projects.next();

            var permissionExists = db.userPermissions.find({entityId:project.projectId, userId:userIds[i]});
            if (!permissionExists.hasNext()) {
                db.userPermission.insert(
                    {
                        entityId: project.projectId,
                        entityType: 'au.org.ala.ecodata.Project',
                        userId: userIds[i],
                        accessLevel: 'caseManager',
                        status:'active'
                    });
            }
        }


    }


}
