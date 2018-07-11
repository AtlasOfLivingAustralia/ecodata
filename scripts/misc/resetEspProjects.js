var ids = ['3b40bc31-34f5-48fa-bfe4-d73fc5da45ae'];
for (var i=0; ids.length; i++) {
    var activities = db.activity.find({projectId:ids[i], progress:{$ne:'planned'}});
    while (activities.hasNext()) {
        var activity = activities.next();

        print("Updating activity "+activity.activityId);
        activity.progress = 'planned';

        db.output.update({activityId:activity.activityId}, {$set:{status:'deleted'}}, {multi:true});
        db.activity.save(activity);
    }

}