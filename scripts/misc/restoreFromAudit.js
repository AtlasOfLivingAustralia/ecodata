var start = ISODate('2019-12-16T02:25:50Z');
var end = ISODate('2019-12-16T02:26:05Z');
var userId = '88141';
var messages = db.auditMessage.find({date:{$gt:start, $lt:end}, userId:userId});
var count = 0;
var documents = [];
while (messages.hasNext()) {
    var message = messages.next();

    var previousEntry = db.auditMessage.find({entityId:message.entityId, date:{$lt:start}}).sort({date:-1});



    var toRestore = previousEntry.next();

    print("Found entry for change: "+message.entityId+", "+message.date+" previous edit date: "+toRestore.date);
    count++;
    var collection;
    if (toRestore.entityType == 'au.org.ala.ecodata.Project') {
        collection = db.project;
    }
    else if (toRestore.entityType == 'au.org.ala.ecodata.Document') {
        var filename = toRestore.entity.filepath+'/'+toRestore.entity.filename;
        documents.push(filename);
        collection = db.document;
    }
    else if (toRestore.entityType == 'au.org.ala.ecodata.Output') {
        collection = db.output;
    }
    else if (toRestore.entityType == 'au.org.ala.ecodata.Activity') {
        collection = db.activity;
    }
    else if (toRestore.entityType == 'au.org.ala.ecodata.Report') {
        collection = db.report;
    }

    var entity = toRestore.entity;

    collection.save(entity);
    var auditMessage = {
        date:ISODate(),
        entity:entity,
        eventType:'Update',
        entityType:toRestore.entityType,
        entityId:toRestore.entityId,
        userId:'1493'
    };
    if (toRestore.projectId) {
        auditMessage.projectId = toRestore.projectId;
    }
    db.auditMessage.insert(auditMessage);

}

print("Found "+count+" messages");
db.project.update({projectId:'d31e6101-2d60-4351-b1ee-64f689f1d48f'}, {$set:{programId:'29d97e8b-e0df-494a-a7ab-f7f102d852c9', managementUnitId:'d2c7573b-4c84-4297-b47b-f504c993b197'}});

for (var i=0; i<documents.length; i++) {
    print("mv '/data/ecodata/archive/"+documents[i]+ "' '/data/ecodata/uploads/"+documents[i]+"'");
}