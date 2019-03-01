var project = db.project.find({projectId:"dfb90f53-3a3d-4b48-b2d8-51487c04b64c"}).next();

var activities = db.activity.find({projectId:project.projectId});

var adminUserId = '1493';

function audit(entity, entityId, entityType, eventType) {
    eventType = eventType || 'Update';

    var auditMessage = {
        date:ISODate(),
        entity:entity,
        eventType:eventType,
        entityType:entityType,
        entityId:entity._id,
        userId:adminUserId
    };
    if (entity.projectId) {
        auditMessage.projectId = entity.projectId;
    }

    db.auditMessage.insert(auditMessage);
}

var activity1Id = '533eae9f-a89c-4ee9-97d1-7bc293d9af22';
var activity2Id = '507f7a7d-8fb5-46a4-a5fb-f860e5b03181';

var activity1 = db.activity.find({activityId:activity1Id}).next();
var activity2 = db.activity.find({activityId:activity2Id}).next();

if (activity1.name != activity2.name) {
    throw "Activity 1 & Activity 2 are of different types!";
}
if (activity1.plannedEndDate > activity2.plannedEndDate) {
    throw "Activity 1 finishes after activity 2";
}

var outputs = db.output.find({activityId:activity2Id});
while (outputs.hasNext()) {
    var output = outputs.next();
    output.activityId = activity1.activityId;
    db.output.save(output);
    audit(output, output.outputId, 'au.org.ala.ecodata.Output');
}

var documents = db.document.find({activityId:activity2Id});
while (documents.hasNext()) {
    var document = documents.next();
    document.activityId = activity1.activityId;
    db.document.save(document);
    audit(document, document.documentId, 'au.org.ala.ecodata.Document');
}

// Now update.
var activity2Progress = activity2.progress;
activity2.progress = activity1.progress;
activity1.progress = activity2Progress;

db.activity.save(activity1);
audit(activity1, activity1.activityId, 'au.org.ala.ecodata.Activity');

db.activity.save(activity2);
audit(activity2, activity2.activityId, 'au.org.ala.ecodata.Activity');









