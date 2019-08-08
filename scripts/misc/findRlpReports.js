var projects = db.project.find({programId:{$exists:true}, status:{$ne:'deleted'}, lastUpdated:{$gt:ISODate("2019-05-28T14:00:00Z")}});

print("ProjectId, Project Name, Report name, Report Start, Report End");
while (projects.hasNext()) {
    var project = projects.next();

    var auditMessages = db.auditMessage.find({entityId:project.projectId}).sort({date:-1});
    if (auditMessages.hasNext()) {
        var audit = auditMessages.next();

        if (auditMessages.hasNext()) {
            var audit2 = auditMessages.next();

            while (auditMessages.hasNext() && audit2.date.getTime() > ISODate("2019-05-28T14:00:00Z").getTime()) {
                audit2 = auditMessages.next();
            }

            if (audit2.entity.plannedStartDate) {
                if (audit2.entity.plannedStartDate.getTime() != audit.entity.plannedStartDate.getTime()) {
                    // print(project.projectId);
                    // print(project.name);
                    // print(audit2.entity.plannedStartDate);
                    // print(audit.entity.plannedStartDate);



                    var reports = db.report.find({projectId:project.projectId, lastUpdated:{$gt:ISODate("2019-05-25T14:00:00Z")}});

                    while (reports.hasNext()) {
                        var report = reports.next();

                        var status = null;
                        switch (report.publicationStatus) {
                            case "published":
                                status = "approved";
                                break;
                            case "pendingApproval":
                                status = "submitted";
                                break;
                        }

                        if (!status) {
                            var activity = db.activity.find({activityId:report.activityId});

                            if (activity.hasNext()) {
                                var activity = activity.next();
                                status = activity.progress;
                            }
                            else {
                                status = 'not started';
                            }
                        }

                        if (status != 'planned') {
                            print(project.projectId+",\""+project.name+"\", \""+report.name+"\","+report.fromDate+","+report.toDate+","+status+","+report.lastUpdated+","+ report.status);
                        }


                    }

                }
            }

        }
    }
}