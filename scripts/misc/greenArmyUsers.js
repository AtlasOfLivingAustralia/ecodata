var config = [{
    org:'Job Futures',
    users:[{userId:'9789', role:'admin'}]
}, {
    org:'Manpower',
    users:[{userId:'7346', role:'caseManager'}, {userId:'10273', role:'admin'}]
}];

for (var i=0; i<config.length; i++) {
    var conf = config[i];
    var projects = db.project.find({serviceProviderName:conf.org});
    while (projects.hasNext()) {
        var project = projects.next();

        for (var j=0; j<conf.users.length; j++) {
            var exists = db.userPermission.find(
                {
                    userId:conf.users[j].userId,
                    accessLevel:conf.users[j].role,
                    entityType:'au.org.ala.ecodata.Project',
                    entityId:project.projectId
                }
            );
            if (exists.count() == 0) {
                print("adding "+conf.users[j].userId+" to "+project.name);

                db.userPermission.insert(
                    {
                        userId: conf.users[j].userId,
                        accessLevel: conf.users[j].role,
                        entityType: 'au.org.ala.ecodata.Project',
                        entityId: project.projectId
                    }
                );
            }
        }
    }

}