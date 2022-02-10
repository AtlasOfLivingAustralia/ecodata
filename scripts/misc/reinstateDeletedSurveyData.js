// Re instate deleted Project Activities (Activity, Output, Document and Record)

// Example:
// cd /Users/sat01a/All/j2ee/mongodb_4.0.21/bin
// ./mongo -u ecodata -p password ecodata ~/reinstateDeletedSurveyData.js

// Test id: b9a20754-2312-4701-b431-37f4f00db06d
// Prod id: 24858b81-8784-4108-9a93-cd99d24d4cbf
var projectActivityId = 'a6ed81d1-d26b-49b9-a730-399b4b02e7d0';
// Time added to query to include only bulk deleted activities.
var timeSelector = {$gt: ISODate("2022-01-26 00:07:39.049Z")}
var activitySelector = {projectActivityId:projectActivityId, status: "deleted", lastUpdated: timeSelector}
print('Total activities to update : ' + db.activity.find(activitySelector).count());
var activities = db.activity.find(activitySelector);
var count = 0;
print("Updating activities...");
while (activities.hasNext()) {
    var activity = activities.next();
    var outputs = db.output.find({activityId: activity.activityId});

    while (outputs.hasNext()) {
        var output = outputs.next();
        // Update documents status for fhe given outputId
        db.document.update({outputId: output.outputId},{$set:{status:'active'}},{multi:true});

        // Update record status
        db.record.update({outputId: output.outputId},{$set:{status:'active'}},{multi:true});

        // Update comment status
        db.comment.update({outputId: output.outputId},{$set:{status:'active'}},{multi:true});
    }

    //Update outputs.
    db.output.update({activityId: activity.activityId},{$set:{status:'active'}},{multi:true});

    //Update documents associated with activity
    db.document.update({activityId: activity.activityId},{$set:{status:'active'}},{multi:true});

    //Update comment associated with activity
    db.comment.update({activityId: activity.activityId},{$set:{status:'active'}},{multi:true});

    count++;
    if (count % 20 == 0) {
        print("Updated " + count + " activities...");
    }
}

db.activity.update(activitySelector,{$set:{status:'active'}},{multi:true});

print("Updated " + count + " activities...");
print('Completed...');