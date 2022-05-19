var reports = db.report.find({'statusChangeHistory.category':{$exists:true}});
while (reports.hasNext()) {
    var report = reports.next();
    for (var i=0; i<report.statusChangeHistory.length; i++) {
        var statusChange = report.statusChangeHistory[i];
        if (statusChange.category && statusChange.category != "") {
            statusChange.categories = [statusChange.category];
        }
        delete statusChange.category;
    }
    db.report.save(report);
}