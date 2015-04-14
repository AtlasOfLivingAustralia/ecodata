var allProjects = db.project.find({status:{$ne:'deleted'}});

while (allProjects.hasNext()) {
    var project = allProjects.next();

    var start = project.plannedStartDate;
    var end = project.plannedEndDate;

    var timeline = project.timeline;
    if (timeline && timeline[0]) {
        var timelineStart = ISODate(timeline[0].fromDate);
        var timelineEnd = ISODate(timeline[timeline.length-1].toDate);

        if (timelineStart != project.plannedStartDate) {
            print("Project "+project.projectId+" has timeline start="+timelineStart+", planned start="+start);
            start = timelineStart;

        }
        if (timelineEnd != project.plannedEndDate) {
            print("Project "+project.projectId+" has timeline end="+timelineEnd+", planned end="+end);
            end = timelineEnd;

        }
    }
    else {
        print("Project "+project.projectId+" has no timeline");
    }

    var beforeActivities = db.activity.find({projectId:project.projectId, plannedEndDate:{$lt:start}, status:{$ne:'deleted'}});
    var afterActivities = db.activity.find({projectId:project.projectId, plannedEndDate:{$gt:end}, status:{$ne:'deleted'}});

    if (beforeActivities.count()) {
        while (beforeActivities.hasNext()) {
            var act = beforeActivities.next();
            print("Project "+project.projectId+" has activity before timeline start="+start+", planned end="+act.plannedEndDate);
        }
    }
    if (afterActivities.count()) {
        while (afterActivities.hasNext()) {
            var act = afterActivities.next();
            print("Project "+project.projectId+" has activity after timeline end="+end+", planned end="+act.plannedEndDate);
        }
    }
}