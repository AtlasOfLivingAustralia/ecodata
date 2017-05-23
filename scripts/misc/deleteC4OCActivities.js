var projects = db.project.find({status:{$ne:'deleted'}, associatedSubProgram:'Regional Delivery 1318'});
var toDate = ISODate("2016-01-01T00:00:00Z");
while (projects.hasNext()) {
    var project = projects.next();

    var reports = db.report.find({projectId:project.projectId, toDate:{$gte:toDate}, status:{$ne:'deleted'}});
    while (reports.hasNext()) {
        var report = reports.next();

        var activities = db.activity.find({
            projectId: project.projectId,
            status: {$ne: 'deleted'},
            plannedEndDate: {$lte: report.toDate, $gt:report.fromDate}
        });

        var allPlanned = true;
        while (activities.hasNext()) {
            var activity = activities.next();

            if (activity.progress !== 'planned') {
                allPlanned = false;
            }
        }
        if (activities.count() > 0 && allPlanned) {
            print(project.projectId+", "+report.name +", "+report.fromDate+", "+report.toDate+",deleted");
        }
        else {
            if (activities.count() > 0) {
                print(project.projectId+", "+report.name +", "+report.fromDate+", "+report.toDate+",non planned activities");
            }
            else {
                print(project.projectId+", "+report.name +", "+report.fromDate+", "+report.toDate+",no activities");
            }
        }
    }

}