
var projects = db.project.find({status:{$ne:'deleted'}, associatedProgram:'Green Army'});

while (projects.hasNext()) {
    var project = projects.next();
    var activities = db.activity.find({projectId:project.projectId});
    var outputCount = 0;
    while (activities.hasNext()) {
        var activity = activities.next();
        var outputs = db.output.find({activityId:activity.activityId});
        outputCount += outputs.count();
    }
    print("Project "+project.projectId+": "+outputCount);
}