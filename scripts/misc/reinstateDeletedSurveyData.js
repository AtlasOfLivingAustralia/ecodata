// Re instate deleted Project Activities (Activity, Output, Document and Record)

// Example:
// cd /Users/sat01a/All/j2ee/mongodb_2.6.2/bin
// ./mongo ~/reinstateDeletedSurveyData.js

var conn = new Mongo();
var ecodataDb = conn.getDB("ecodata");
// Test id: b9a20754-2312-4701-b431-37f4f00db06d
// Prod id: 24858b81-8784-4108-9a93-cd99d24d4cbf
var projectActivityId = '24858b81-8784-4108-9a93-cd99d24d4cbf';
print('Total activities to update : ' + ecodataDb.activity.find({projectActivityId:projectActivityId}).count());
var activities = ecodataDb.activity.find({projectActivityId: projectActivityId});
var count = 0;
print("Updating activities...");
while (activities.hasNext()) {
    var activity = activities.next();
    var outputs = ecodataDb.output.find({activityId: activity.activityId});

    while (outputs.hasNext()) {
        var output = outputs.next();
        // Update documents status for fhe given outputId
        ecodataDb.document.update({outputId: output.outputId},{$set:{status:'active', hosted:'images.ala.org.au'}},{multi:true});

        // Update record status
        ecodataDb.record.update({outputId: output.outputId},{$set:{status:'active'}},{multi:true});
    }

    //Update outputs.
    ecodataDb.output.update({activityId: activity.activityId},{$set:{status:'active'}},{multi:true});

    count++;
    if (count % 20 == 0) {
        print("Updated " + count + " activities...");
    }
}

ecodataDb.activity.update({projectActivityId: projectActivityId},{$set:{status:'active'}},{multi:true});

print("Updated " + count + " activities...");
print('Completed...');