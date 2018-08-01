var userIds = ['6065', '6360', '29926', '7420', '6942', '8138'];

var rlp = db.program.find({name:'Regional Landcare Program'}).next();
var mus = db.program.find({parent:rlp._id});
while (mus.hasNext()) {
    var mu = mus.next();
    print('"'+mu.name+'",'+'https://fieldcapture.ala.org.au/rlp/index/'+mu.programId);

    for (var i=0; i<userIds.length; i++) {
        db.userPermission.insert({entityId:mu.programId, entityType:'au.org.ala.ecodata.Program', userId:userIds[i], accessLevel:'caseManager'});
    }
}