var messages = db.auditMessage.find({entityId:'ffb0ec39-4d4c-45fe-8689-c6d73d7f55e3'});
while (messages.hasNext()) {
    var message = messages.next();

    var changed = false;
    if (message.entity.data && message.entity.data.weedTreatmentSites) {
        for (var i=0; i<message.entity.data.weedTreatmentSites.length; i++) {
            if (message.entity.data.weedTreatmentSites[i].weedSpeciesTreated) {
                for (var j=0; j<message.entity.data.weedTreatmentSites[i].weedSpeciesTreated.length; j++) {
                    if (!message.entity.data.weedTreatmentSites[i].weedSpeciesTreated[j].otherTreatmentMethod) {
                        delete message.entity.data.weedTreatmentSites[i].weedSpeciesTreated[j].otherTreatmentMethod;
                        changed = true;
                    }
                }
            }
        }
    }
    if (changed) {
        print("Updating auditMessage "+message._id);
        db.auditMessage.save(message);
    }
}