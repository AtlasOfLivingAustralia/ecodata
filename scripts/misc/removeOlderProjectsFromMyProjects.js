var userId = '';
var olderThan = ISODate('2020-01-02T00:00:00Z');

// List of reef programs, chosen after discussion with affected user.
var programs = [
    "768194e1-eaa4-47e2-a21e-6662b694bd1c",
    "36ba6dec-200a-40d0-a646-0103788a88aa",
    "19ca215b-011d-4f83-af4a-c0110b6fd489",
    "5c750165-1b9e-4604-b903-6b1167611ea8"
];
var projects = db.userPermission.find({userId:userId});
while (projects.hasNext()) {
    var project = projects.next();
    if (project.entityType == 'au.org.ala.ecodata.Project')  {
        var p = db.project.find({projectId:project.entityId});
        if (p.hasNext()) {
            var prj = p.next();
            if (prj.plannedEndDate.getTime() < olderThan || programs.indexOf(prj.programId) >=0) {
                print(prj.plannedEndDate +" "+ prj.name);
                db.userPermission.remove(project);
            }
        }
        else {
            db.userPermission.remove(project);
            print("No matching project for : "+project.entityId);
        }
    }
}