var userId = '';
var olderThan = ISODate('2020-01-02T00:00:00Z');
var projects = db.userPermission.find({userId:userId});
while (projects.hasNext()) {
    var project = projects.next();
    if (project.entityType == 'au.org.ala.ecodata.Project')  {
        var p = db.project.find({projectId:project.entityId}).next();

        if (p.plannedEndDate.getTime() < olderThan) {
            print(p.plannedEndDate);
            db.userPermission.remove(project);
        }
    }
}