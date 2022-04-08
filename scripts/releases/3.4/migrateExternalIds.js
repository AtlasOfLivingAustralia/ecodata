
function addExternalId(project, type, value) {
    if (!project.externalIds) {
        project.externalIds = [];
    }
    project.externalIds.push({idType:type, externalId: value});
}

var projects = db.project.find({status:{$ne:'deleted'}, isMERIT:true});
while (projects.hasNext()) {
    var project = projects.next();
    var changed = false;
    if (project.workOrderId) {
        addExternalId(project, 'WORK_ORDER', project.workOrderId);
        delete project.workOrderId;
        changed = true;
    }
    if (project.internalOrderId) {
        addExternalId(project, 'INTERNAL_ORDER_NUMBER', project.internalOrderId);
        delete project.internalOrderId;
        changed = true;
    }

    if (changed) {
        db.project.save(project);
    }
}
