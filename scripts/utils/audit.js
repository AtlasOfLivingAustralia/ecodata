/** Inserts a document into the auditMessage collection */
function audit(entity, entityId, type, userId, projectId, eventType) {
    var auditMessage = {
        date: ISODate(),
        entity: entity,
        eventType: eventType || 'Update',
        entityType: type,
        entityId: entityId,
        userId: userId
    };
    if (entity.projectId || projectId) {
        auditMessage.projectId = (entity.projectId || projectId);
    }
    db.auditMessage.insertOne(auditMessage);
}

/** Updates the name and dates for a supplied report */
function updateReportDetails(reportId, name, fromDate, toDate, userId, submissionDate, description) {
    var now = ISODate();
    var report = db.report.findOne({reportId:reportId});
    report.fromDate = fromDate;
    report.toDate = toDate;
    report.lastUpdated = now;
    report.name = name;
    report.description = description || name;
    report.submissionDate = submissionDate || toDate;

    db.report.replaceOne({_id: report._id}, report);
    audit(report, report.reportId, 'au.org.ala.ecodata.Report', userId);

    updateActivity(report, userId);
}

/** Updates an activity to match the changes made to a supplied report and audits the change */
function updateActivity(report, userId)  {
    var activity = db.activity.findOne({activityId:report.activityId});
    activity.plannedEndDate = report.toDate;
    activity.endDate  =  report.toDate;
    activity.plannedStartDate = report.fromDate;
    activity.startDate = report.fromDate;
    activity.description = report.name;

    activity.lastUpdated = report.lastUpdated;

    db.activity.replaceOne({_id:activity._id}, activity);
    audit(activity, activity.activityId, 'au.org.ala.ecodata.Activity', userId);
}

function addProjectPermission(userId, projectId, accessLevel, adminUserId) {
    addPermission(userId, projectId, accessLevel, 'au.org.ala.ecodata.Project', adminUserId);
}

function addPermission(userId, entityId, accessLevel, entityType, adminUserId) {
    var userPermission = {
        userId:userId,
        entityType:entityType,
        entityId:entityId,
        accessLevel:accessLevel
    };
    if (db.userPermission.findOne({userId:userId, entityId:entityId})) {
        print("Not adding permission for user: "+userId+" to entity: "+entityId+", permission already exists");
    }
    else {
        print("Adding permission for user: "+userId+" to entity: "+entityId+" of type: "+entityType);
        db.userPermission.insert(userPermission);
        var id = db.userPermission.findOne({userId:userId, entityId:entityId})._id;
        audit(userPermission, id, 'au.org.ala.ecodata.UserPermission', adminUserId, entityId);
    }

}


function restoreRemovedPermissions(userId, removalDate, adminUserId) {
    let messages = db.auditMessage.find(
        {'entity.userId':userId,
            userId:'<anon>',
            entityType:'au.org.ala.ecodata.UserPermission',
            date:{$gt:ISODate(removalDate)},
            eventType:"Delete"
        });
    while (messages.hasNext()) {
        let message = messages.next();
        let permission = message.entity;

        addProjectPermission(userId, permission.entityId, permission.accessLevel, permission.entityType, adminUserId);

    }
}