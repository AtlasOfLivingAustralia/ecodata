var projects = db.project.find({status:'active'});
while (projects.hasNext()) {
    var project = projects.next();

    var reports = db.report.find({projectId:project.projectId}).sort({'toDate': -1});

    if (reports.count() == 0) {
        print("No reports for project: "+project.projectId);
    }
    else {
        var report = reports.next();
        if (report.fromDate > project.plannedEndDate) {
            print("Report after project end for project "+project.projectId+' project end: '+project.plannedEndDate+' report from date '+report.fromDate);

        }
    }

}