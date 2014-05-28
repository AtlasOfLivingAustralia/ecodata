
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