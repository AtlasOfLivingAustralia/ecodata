load('uuid.js');
load("projectIdsAndIO.js")
var  adminUserId  = '94320';
function audit(entity, entityId, type) {
    var auditMessage = {
        date: ISODate(),
        entity: entity,
        eventType: 'Update',
        entityType: type,
        entityId: entityId,
        userId: adminUserId,
        projectId: entity.projectId
    };
    db.auditMessage.insert(auditMessage);
}
for (var i =0; i<projectData.length; i++) {
    var project = db.project.find({projectId:projectData[i]});
    if (project.hasNext()){
        db.project.update({projectId:projectData[i]}, {$set:{internalOrderId:projectData[i+1]}});
        var p = db.project.find({projectId:projectData[i]}).next();
        audit(p, p.projectId, "au.org.ala.ecodata.Project");

    }else{
        print("No project found with id: " + projectData[i]);
    }
}

