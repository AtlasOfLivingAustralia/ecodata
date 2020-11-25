var reports = db.report.find({submissionDeltaInWeekdays:{$exists:true}});
while (reports.hasNext()) {
    var report = reports.next();

    report.submissionDeltaInWeekdays = NumberInt(report.submissionDeltaInWeekdays);
    db.report.save(report);
}

var reports = db.report.find({approvalDeltaInWeekdays:{$exists:true}});
while (reports.hasNext()) {
    var report = reports.next();

    report.approvalDeltaInWeekdays = NumberInt(report.approvalDeltaInWeekdays);
    db.report.save(report);

}