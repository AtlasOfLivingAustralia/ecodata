var orgs = db.project.distinct('organisationName')
for (var i=0; i<orgs.length; i++) {

    var orgUsers = {};
    var projects = db.project.find({organisationName:orgs[i], status:{$ne:'deleted'}});
    while (projects.hasNext()) {
        var project = projects.next();

        var permissions = db.userPermission.find({entityId:project.projectId});
        while (permissions.hasNext()) {
            var permission = permissions.next();
            if (permission.accessLevel=='caseManager') {
                orgUsers[permission.userId] = true;
            }
        }
    }
    var line = '"'+orgs[i]+'"';
    var userCount = 0;
    for (var user in orgUsers) {
        if (orgUsers.hasOwnProperty(user)) {
            userCount++;
        }
    }
    print(line+','+projects.count()+','+userCount);

}