var toRemove = ['Co-operative Management Area','South NRM Region - Macquarie Islands', 'North Coast - Lord Howe Island'];
for (var i=0; i<toRemove.length; i++) {


    var program = db.program.find({name:toRemove[i]});
    if (program.hasNext()) {
        var p = program.next();
        print("Removing program: "+toRemove[i]);
        db.userPermission.remove({entityId:p.programId});
        db.program.remove({programId:p.programId});
    }
    else {
        print("Cannot find program with name: "+toRemove[i]);
    }

}
