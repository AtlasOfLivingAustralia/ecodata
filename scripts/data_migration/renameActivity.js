
function renameActivity(oldName, newName) {
    var beforeOld = db.activity.find({type:oldName}).count();
    if (beforeOld == 0) {
        print("No activities currently exist with name: "+oldName);
        return;
    }
    var beforeNew = db.activity.find({type:newName}).count();
    db.activity.update({type:oldName}, {$set:{type:newName}}, false, true);
    var afterOld = db.activity.find({type:oldName}).count();
    var afterNew = db.activity.find({type:newName}).count();

    if (afterOld != 0) {
        throw {name:'Fatal error', message:'Not all activities renamed from '+oldName+' to '+newName};
    }
    if ((afterNew-beforeNew) != beforeOld) {
        throw {name:'Fatal error', message:'Not all activities renamed from '+oldName+' to '+newName};
    }

    print("Renamed "+beforeOld+" activities from "+oldName+" to "+newName);
}

function renameActivityWithoutOutputs(oldName, newName) {
    var beforeOld = db.activity.find({type:oldName}).count();
    if (beforeOld == 0) {
        print("No activities currently exist with name: "+oldName);
        return;
    }

    var count = 0;
    var activities = db.activity.find({type: oldName});
    var noOutputCount = 0;
    while (activities.hasNext()) {
        var activity = activities.next();
        var outputs = db.output.find({activityId: activity.activityId}).count();
        if (outputs > 0) {
            print(type+','+activity.projectId+','+activity.activityId+',Has outputs - not changing');
            count++;
        }
        else {
            activity.type = newName;
            db.activity.save(activity);
            noOutputCount++;

        }
    }

    if ((count + noOutputCount) != beforeOld) {
        throw {name:'Fatal error', message:'Not all activities processed from '+oldName+' to '+newName};
    }

    print("Renamed "+noOutputCount+" activities from "+oldName+" to "+newName);
    print("Left "+count+" activities with "+oldName+" due to outputs present");
}