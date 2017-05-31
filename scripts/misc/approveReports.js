var reports = db.report.find({organisationId:{$exists:true}, type:'Performance Management Framework - Self Assessment', publicationStatus:'pendingApproval'});

var now = ISODate();
var user = '11336';

while (reports.hasNext()) {



    var report = reports.next();

    report.approvedBy = user;
    report.dateApproved = now;
    report.publicationStatus = 'published';

    var statusChange = {
        "category" : "",
        "changedBy" : user,
        "dateChanged" : now,
        "status" : "approved",
        "comment": "Confirmed by the Department of the Environment and Energy"
    };

    report.statusChangeHistory.push(statusChange);


    print("Updating report with id: "+report.reportId);

    db.report.save(report);
}