var activities = db.activity.find({lastUpdated:{$gt:new Date('2015-07-01T00:00:00Z')}});
print('Project,Programme,Sub-Programme,start date, end date, type, status');
while (activities.hasNext()) {
    var activity = activities.next();
    var project = db.project.find({projectId:activity.projectId}).next();

    print(project.projectId+','+project.associatedProgram+','+project.associatedSubProgram+','+activity.plannedStartDate+','+activity.plannedEndDate+','+activity.type+','+activity.status)

}
