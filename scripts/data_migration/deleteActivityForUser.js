// cd /Users/sat01a/All/j2ee/mongodb_2.6.2/bin
// ./mongo /Users/sat01a/All/sat01a_git/merged/ecodata-2/scripts/data_migration/deleteActivityForUser.js
// > http://biocollect.ala.org.au/project/index/88f6c3b8-e2ab-4f04-97bc-881e84204bfc
// > Alan Stenhouse = 28106

var projectName = 'Great Koala Count 2';
var projectActivityName = 'Great SA Koala Count 2016';
var userId = "28106";
var max = 10;

//var projectName = 'Test';
//var projectActivityName = 'Survey name 1';
//var userId = "8443";
//var max = 2;

print('Deleting user activities...');

var sightingsConn = new Mongo();
var ecodataDb = sightingsConn.getDB("ecodata");

var project = ecodataDb.project.find({name: projectName}).next();
var projectActivity = ecodataDb.projectActivity.find({$and : [{projectId: project.projectId, name: projectActivityName}]}).next();
var totalActivities = ecodataDb.activity.find({$and: [{userId:userId, projectId:project.projectId, projectActivityId: projectActivity.projectActivityId, status:'active'}]}).count();

var counter = 0;
print('Total activties to delete : ' + totalActivities);

while (totalActivities > counter) {

    var activities = ecodataDb.activity.find({$and: [{userId:userId, projectId:project.projectId, projectActivityId: projectActivity.projectActivityId, status:'active'}]}).limit(max);

    while (activities.hasNext()) {
        var activity = activities.next();
        ecodataDb.output.update({activityId:activity.activityId},{$set:{status:'deleted'}});
        ecodataDb.record.update({activityId:activity.activityId},{$set:{status:'deleted'}});
        ecodataDb.document.update({activityId:activity.activityId},{$set:{status:'deleted'}});
        ecodataDb.activity.update({activityId:activity.activityId},{$set:{status:'deleted'}});
        counter++;
    }

    print('Successfully deleted '+ counter + ' records');
}
