
var reports = db.report.find({status:{$ne:'deleted'}}).sort({projectId:1, toDate:-1});
while (reports.hasNext()) {

    var report = reports.next();

    if (report.projectId) {
        print("Checking report "+report.name+" for project "+report.projectId);

        var activities = db.activity.find({projectId:report.projectId, plannedEndDate:{$gt:report.fromDate, $lte:report.toDate}, publicationStatus:{$ne:report.publicationStatus}, status:{$ne:'deleted'}});

        while (activities.hasNext()) {
            var activity = activities.next();

            var reportStatus = report.publicationStatus || 'unpublished';
            var activityStatus = activity.publicationStatus || 'unpublished';

            if (reportStatus != activityStatus) {
                print("Found activity: "+activity.activityId+" with status "+activity.publicationStatus+ " not matching report status "+report.publicationStatus+" last updated: "+activity.lastUpdated);
            }
        }

    }


}