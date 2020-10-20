
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

function updateActivity(report)  {
    var activity = db.activity.find({activityId:report.activityId}).next();
    activity.plannedEndDate = report.toDate;
    activity.endDate  =  report.toDate;
    activity.plannedStartDate = report.fromDate;
    activity.startDate = report.fromDate;
    activity.description = report.name;

    activity.lastUpdated = report.lastUpdated;

    db.activity.save(activity);
    audit(activity, activity.activityId, 'au.org.ala.ecodata.Activity');
}

var projectId = 'd31e6101-2d60-4351-b1ee-64f689f1d48f';
/*
var report = db.report.find({reportId:'a875fa51-c64e-47e0-a9f5-b64700f36ece'}).next();
report.name = 'Year 2018/2019 Quarter 2 Outputs Report';
report.description = 'Year 2018/2019 Quarter 2 Outputs Report';
report.lastUpdated = ISODate();
db.report.save(report);
audit(report, report.reportId, 'au.org.ala.ecodata.Report');
updateActivity(report);

report = db.report.find({reportId:'2ca3704e-9237-4f5e-b987-31b568345e5a'}).next();
report.toDate = ISODate("2019-03-31T13:00:00Z");
report.submissionDate = report.fromDate;
report.name = 'Year 2018/2019 Quarter 3 Outputs Report';
report.description = 'Year 2018/2019 Quarter 3 Outputs Report';
report.lastUpdated = ISODate();
db.report.save(report);
audit(report, report.reportId, 'au.org.ala.ecodata.Report');
updateActivity(report);

report = db.report.find({reportId:'c75dc085-3aa3-4493-8c95-f798c8da1847'}).next();
report.fromDate = ISODate("2019-03-31T13:00:00Z");
report.toDate = ISODate("2019-06-30T14:00:00Z");
report.submissionDate = report.fromDate;
report.name = 'Year 2018/2019 Quarter 4 Outputs Report';
report.description = 'Year 2018/2019 Quarter 4 Outputs Report';
report.lastUpdated = ISODate();
db.report.save(report);
audit(report, report.reportId, 'au.org.ala.ecodata.Report');
updateActivity(report);

report = db.report.find({reportId:'b4a31e90-a8a5-41f8-bec5-0fea89ad48a8'}).next();
report.fromDate = ISODate("2019-06-30T14:00:00Z");
report.toDate = ISODate("2019-12-31T13:00:00Z");
report.submissionDate = report.fromDate;
report.name = 'Year 2019/2020 - Semester 1 Outputs Report';
report.description = 'Year 2019/2020 - Semester 1 Outputs Report';
report.lastUpdated = ISODate();
db.report.save(report);
audit(report, report.reportId, 'au.org.ala.ecodata.Report');
updateActivity(report);

report = db.report.find({reportId:'bbd6da1d-e6f4-4e32-a11b-7f628e3346f8'}).next();
report.fromDate = ISODate("2019-12-31T13:00:00Z");
report.toDate = ISODate("2020-06-30T14:00:00Z");
report.submissionDate = report.fromDate;
report.name = 'Year 2019/2020 - Semester 2 Outputs Report';
report.description = 'Year 2019/2020 - Semester 2 Outputs Report';
report.lastUpdated = ISODate();
db.report.save(report);
audit(report, report.reportId, 'au.org.ala.ecodata.Report');
updateActivity(report);
*/
//db.report.save(previousReport);
//audit(previousReport, previousReport.reportId, 'au.org.ala.ecodata.Report');
//db.report.update({reportId:'051bd9cf-c416-47a8-a8a1-1df31228c4b1'},{$set:{status:'deleted'}});
//db.activity.update({activityId:'cb162c8d-2a92-4da7-87a4-b3ea46548d4b'},{$set:{status:'deleted'}});




var reports = db.report.find({projectId:projectId, activityType:'RLP Output Report', status:{$ne:'deleted'}}).sort({toDate:1});

var currentReport;
var currentActivity;

var previousReport;
var previousActivity;

while (reports.hasNext()) {

    var previousReport = reports.next();
    var copyOfPreviousReport = db.report.find({reportId:previousReport.reportId}).next();

    var previousActivity = db.activity.find({activityId:previousReport.activityId}).next();
    var copyOfPreviousActivity = db.activity.find({activityId:previousReport.activityId}).next();

    print("current: "+(currentReport && currentReport.name));
    print("previous: "+previousReport.name);
    print("********");
    if (currentReport && previousReport) {

        print("Moving "+currentReport.name+" to "+previousReport.name);

        previousReport.name = currentReport.name;
        previousReport.description = currentReport.description;
        previousReport.toDate = currentReport.toDate;
        previousReport.fromDate = currentReport.fromDate;
        previousReport.lastUpdated = ISODate();
        previousReport.submissionDate = currentReport.submissionDate;

        db.report.save(previousReport);
        audit(previousReport, previousReport.reportId, 'au.org.ala.ecodata.Report');

        previousActivity.plannedStartDate = previousReport.fromDate;
        previousActivity.startDate = previousReport.fromDate;
        previousActivity.plannedEndDate = previousReport.toDate;
        previousActivity.endDate = previousReport.toDate;
        previousActivity.lastUpdated = ISODate();
        previousActivity.description = currentActivity.description;
        db.activity.save(previousActivity);
        audit(previousActivity, previousActivity.activityId, 'au.org.ala.ecodata.Activity');



    }

    currentReport = copyOfPreviousReport;
    currentActivity = copyOfPreviousActivity;
}

var toDeleteReport = db.report.find({reportId:'eb290d93-f30e-4094-821d-ce5d98b7428f'}).next();
db.report.update({reportId:'eb290d93-f30e-4094-821d-ce5d98b7428f'},{$set:{status:'deleted'}});

db.activity.update({activityId:toDeleteReport.activityId},{$set:{status:'deleted'}});
db.output.update({activityId:toDeleteReport.activityId},{$set:{status:'deleted'}});