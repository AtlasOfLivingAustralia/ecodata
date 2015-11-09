var activities = db.activity.find({type:/Green Army - .*report/});
while (activities.hasNext()) {
    var activity = activities.next();
    db.output.update({activityId:activity.activityId}, {$set:{status:'deleted'}}, true, true);
}
db.activity.update({type:/Green Army - .*report/}, {$set:{status:'deleted'}}, true, true);