load('../../utils/audit.js');
let adminUserId = 'system';
let count = 0;
db.activity.find({"description" : "Activity submitted by monitor"}).forEach(function(activity) {
    if (!activity.externalIds || activity.externalIds.length == 0) {
        var project = db.project.findOne({projectId: activity.projectId});
        if (project && project.custom && project.custom.dataSets) {
            var dataset = project.custom.dataSets.find(function(dataset) {
                return dataset.activityId == activity.activityId;
            });

            if (dataset) {
                activity.externalIds = [{idType: "MONITOR_MINTED_COLLECTION_ID", externalId: dataset.dataSetId}];
                db.activity.replaceOne({activityId: activity.activityId}, activity);
                audit(activity, activity.activityId, 'au.org.ala.ecodata.Activity', adminUserId);
                print(`Updated activity:  + ${activity.activityId} +  with dataSetId: ${dataset.dataSetId}`);
                count ++;
            }
            else {
                print(`Activity: ${activity.activityId} does not have a dataset`);
            }
        }
    }
});

print(`Updated ${count} activities`);