var projects = db.project.find({associatedProgram:'Environmental Stewardship', name:/Environmental Stewardship Program project on property/, status:{$ne:'deleted'}});

var day = 1000*60*60*24;
while (projects.hasNext()) {
    var project = projects.next();
    if (project.plannedStartDate.getDate() != 1) {

        print("Matched project: "+project.name+" with start date: "+project.plannedStartDate);
        project.plannedStartDate = new Date(project.plannedStartDate.getTime() + day);
        db.project.save(project);

        var reports = db.report.find({projectId:project.projectId, status:"active"});
        while (reports.hasNext()) {
            var report = reports.next();
            report.fromDate = new Date(report.fromDate.getTime()+day);
            report.toDate = new Date(report.toDate.getTime()+day);

            db.report.save(report);
        }
        var activities = db.activity.find({projectId:project.projectId, status:"active"});
        while (activities.hasNext()) {
            var activity = activities.next();

            activity.plannedStartDate = new Date(activity.plannedStartDate.getTime()+day);
            activity.plannedEndDate = new Date(activity.plannedEndDate.getTime()+day);
            activity.startDate = new Date(activity.startDate.getTime()+day);
            activity.endDate = new Date(activity.endDate.getTime()+day);

            db.activity.save(activity);
        }
    }

}
