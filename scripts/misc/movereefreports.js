load('uuid.js');
var activities = db.activity.find({type:'Reef 2050 Plan Action Reporting', progress:'finished'});
while (activities.hasNext()) {
    var activity = activities.next();

    var output = db.output.find({activityId:activity.activityId}).next();

    var nextActivity = db.activity.find({projectId:activity.projectId, type:'Reef 2050 Plan Action Reporting', plannedEndDate:ISODate("2016-12-30T13:00:00Z")});

    if (nextActivity.hasNext()) {
        nextActivity = nextActivity.next();
    }

    delete output._id;
    output.activityId = nextActivity.activityId;
    output.outputId = UUID.generate();

    db.output.insert(output);


}