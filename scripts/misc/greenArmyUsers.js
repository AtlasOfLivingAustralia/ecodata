var config = [{
    org:'CoAct',
    users:[{userId:'18751', role:'admin'}, {userId:'18749', role:'admin'}]
}];

for (var i=0; i<config.length; i++) {
    var conf = config[i];
    var queries = [{serviceProviderName:conf.org}, {organisationName:conf.org}];
    for (var query=0; query<queries.length; query++) {

        var projects = db.project.find(queries[i]);
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
}