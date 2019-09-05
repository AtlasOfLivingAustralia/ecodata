var userIds = ['52562'];
var addProjects = false;

var rlp = db.program.find({name:'Regional Land Partnerships'}).next();
var mus = db.program.find({parent:rlp._id});
while (mus.hasNext()) {
    var mu = mus.next();
    print('"'+mu.name+'",'+'https://fieldcapture.ala.org.au/rlp/index/'+mu.programId);

    for (var i=0; i<userIds.length; i++) {
        var permissionExists = db.userPermissions.find({entityId:mu.programId, userId:userIds[i]});
        if (!permissionExists.hasNext()) {
            db.userPermission.insert(
                {
                    entityId:mu.programId,
                    entityType:'au.org.ala.ecodata.Program',
                    userId:userIds[i],
                    accessLevel:'caseManager',
                    status:'active'}
            );
        }

        if (addProjects) {
            var projects = db.project.find({programId:mu.programId});

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


}