load('uuid.js');
var projects = db.project.find({associatedSubProgram:'Regional Funding', status:'active'});
while (projects.hasNext()) {
    var project = projects.next();

    var activities = db.activity.find({projectId:project.projectId, status:'active', type:'Regional Funding Final Report'});
    if (activities.count() == 1) {
        print("project "+project.projectId+" already done");
    }
    else {


        activities = db.activity.find({
            projectId: project.projectId,
            status: 'active',
            type: 'Outcomes, Evaluation and Learning - final report'
        });

        if (activities.count() == 0) {
            print("No final report found for " + project.projectId);

            var reports = db.report.find({projectId: project.projectId, status: 'active'}).sort({toDate: -1});

            var report = reports.next();
           var reportEnd = report.toDate.getDate();

            var projectEnd = project.plannedEndDate.getDate();

            if ((projectEnd - reportEnd) < -(1000*60*60*24*7)) {
                print("report after project end for project " + project.projectId+ " stage "+report.name+" end: "+report.toDate+", "+project.plannedEndDate);
            }
            else {
                var activity = {
                    activityId: UUID.generate(),
                    projectId: project.projectId,
                    type: "Regional Funding Final Report",
                    plannedStartDate: report.fromDate,
                    plannedEndDate: project.plannedEndDate,
                    progress: 'planned',
                    status:'active'
                };
                db.activity.insert(activity);
                print("Inserting");
                printjson(activity);
            }
        }
        else if (activities.count() > 1) {
            print("Too many final reports found for " + project.projectId);
            // Pick one...

            var done = false;
            while (!done && activities.hasNext()) {
                var activity = activities.next();
                if (activity.progress != 'planned') {
                    print("Found non planned final report for " + project.projectId);
                }
                else {
                    activity.type = 'Regional Funding Final Report';
                    db.activity.save(activity);
                    done = true;
                }
            }
        }
        else {
            var activity = activities.next();

            if (activity.progress != 'planned') {
                print("Found non planned final report for " + project.projectId);
            }
            else {
                print("All good for " + project.projectId);
                activity.type = 'Regional Funding Final Report';
                db.activity.save(activity);

            }
        }
    }
}