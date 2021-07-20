var messages = db.auditMessage.find({entityId:{$type:'objectId'}, status:{$ne:'deleted'}});
var count = 0;
while (messages.hasNext()) {
    var message = messages.next();
    message.entityId = message.entityId.toString();
    db.auditMessage.save(message);
    count++;
}

print("Updated "+count+" audit messages");