var reports = db.report.find({activityType:'RLP Output Report', publicationStatus:'unpublished', status:{$ne:'deleted'}});
while (reports.hasNext()) {
    var report = reports.next();
    report.submissionDate = report.fromDate;
    db.report.save(report);
}

var mus = db.program.find();
while (mus.hasNext()) {
    var mu = mus.next();
    if (mu.config && mu.config.projectReports) {
        var projectReports = mu.config.projectReports;
        for (var i=0; i<projectReports.length; i++) {
            if (projectReports[i].activityType == 'RLP Output Report') {
                projectReports[i].canSubmitDuringReportingPeriod = true;
                db.program.save(mu);
                break;
            }
        }
    }
}