
var  adminUserId  = '1493';

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



var grantId = 'RLP-MU19-P4';
var reportIdToDelete = '003951ea-b263-42ab-8c9e-c2fb7821d74f';
var reportIdToModifyStartDate = '860af025-df0a-4071-908a-f9d546c5d24c';
var newStartDate = ISODate('2018-11-11T13:00:00Z');

var project = db.project.find({grantId:grantId}).next();

// Delete the first reports
var reportToDelete = db.report.find({reportId:reportIdToDelete}).next();
var activityToDelete =  db.activity.find({activityId:reportToDelete.activityId}).next();
activityToDelete.status = 'deleted';
activityToDelete.lastUpdated = ISODate();
db.activity.save(activityToDelete);
audit(activityToDelete, activityToDelete.activityId, 'au.org.ala.ecodata.Activity');

reportToDelete.status = 'deleted';
reportToDelete.lastUpdated  = ISODate();
db.report.save(reportToDelete);
audit(reportToDelete, reportToDelete.reportId, 'au.org.ala.ecodata.Report');

// Modify  start  date  of second report
var reportToModifyStartDate = db.report.find({reportId:reportIdToModifyStartDate}).next();

var activityToModifyStartDate =  db.activity.find({activityId:reportToModifyStartDate.activityId}).next();
activityToModifyStartDate.plannedStartDate = newStartDate;
activityToModifyStartDate.lastUpdated = ISODate();
db.activity.save(activityToModifyStartDate)
audit(activityToModifyStartDate, activityToModifyStartDate.activityId, 'au.org.ala.ecodata.Activity');

reportToModifyStartDate.fromDate = newStartDate;
reportToModifyStartDate.lastUpdated = ISODate();
db.report.save(reportToModifyStartDate);
audit(reportToModifyStartDate, reportToModifyStartDate.reportId, 'au.org.ala.ecodata.Report');

// Modify  start  date  of project
project.plannedStartDate = newStartDate;
project.lastUpdated = ISODate();
db.project.save(project);
audit(project, project.projectId, 'au.org.ala.ecodata.Project');