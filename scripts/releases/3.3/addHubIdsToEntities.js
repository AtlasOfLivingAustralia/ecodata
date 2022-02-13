var meritHubId = db.hub.findOne({urlPath:'merit'}).hubId;
db.project.update({isMERIT:true}, {$set:{hubId:meritHubId}}, {multi:true});
// Only MERIT programs & management units are in the database
db.program.update({}, {$set:{hubId:meritHubId}}, {multi:true});
db.managementUnit.update({}, {$set:{hubId:meritHubId}}, {multi:true});
db.organisation.update({sourceSystem:'merit'},{$set:{hubId:meritHubId}}, {multi:true});

