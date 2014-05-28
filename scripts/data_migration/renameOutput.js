
function renameOutput(oldName, newName) {
    var beforeOld = db.output.find({name:oldName}).count();
    if (beforeOld == 0) {
        print("No outputs currently exist with name: "+oldName);
        return;
    }
    var beforeNew = db.output.find({name:newName}).count();
    db.output.update({name:oldName}, {$set:{name:newName}}, false, true);
    var afterOld = db.output.find({name:oldName}).count();
    var afterNew = db.output.find({name:newName}).count();

    if (afterOld != 0) {
        throw {name:'Fatal error', message:'Not all activities renamed from '+oldName+' to '+newName};
    }
    if ((afterNew-beforeNew) != beforeOld) {
        throw {name:'Fatal error', message:'Not all activities renamed from '+oldName+' to '+newName};
    }

    print("Renamed "+beforeOld+" output from "+oldName+" to "+newName);
}