load('uuid.js');
var projects = db.project.find({associatedSubProgram: 'Regional Funding', status: {$ne: 'deleted'}});
var toReplace = ['Annual Report', 'Stage Report', 'Outcomes, Evaluation and Learning - final report', 'Progress, Outcomes and Learning - stage report'];
var replaceWith = 'Regional Funding Final Report';

var i = 0;
while (projects.hasNext()) {
    var project = projects.next();


    var activities = db.activity.find(
        {
            projectId: project.projectId,
            //plannedEndDate:{$gt:ISODate("2017-06-01T00:00:00Z"), $lt:ISODate("2017-08-01T00:00:00Z")},
            plannedEndDate: {$gt: ISODate("2018-05-01T00:00:00Z")},
            type: {$in: toReplace},
            status: {$ne: 'deleted'}
        });

    if (activities.count() == 0) {

        var isDone = db.activity.find({projectId:project.projectId, type:replaceWith});
        if (isDone.count()== 0) {
            print("No matching activities for project " + project.projectId);

            activities = db.activity.find({projectId:project.projectId, status:{$ne:'deleted'}}).sort({plannedEndDate:-1});
            if (activities.count() > 0) {
                var activity = activities.next();
                if (activity.plannedEndDate.getTime() > ISODate('2018-05-01T00:00:00Z').getTime()) {
                    if (activity.plannedEndDate.getTime() < ISODate('2018-07-02T00:00Z')) {
                        print("Adding new activity for project "+project.projectId);

                        delete activity._id;
                        activity.activityId = UUID.generate();
                        activity.type = replaceWith;
                        activity.description = 'Final Project Report';
                        activity.progress = 'planned';
                        db.activity.insert(activity);

                    }
                    else {
                        print("Has > stage 7");
                    }

                }
                else {
                    print(activities.next().plannedEndDate);
                }


            }
            else {
                print("No activities at all for "+project.projectId);
            }
        }


    }
    else if (activities.count() > 1) {
        print(activities.count()+" activities for project " + project.projectId);

        if (activities.count() == 2) {
            var fixed = false;
            while (activities.hasNext()) {
                var activity = activities.next();

                if (activity.plannedEndDate.getTime() > ISODate('2018-05-01T00:00:00Z').getTime()
                    && activity.plannedEndDate.getTime() < ISODate('2018-07-02T00:00Z')
                    && activity.progress == 'planned') {

                    if (!fixed) {
                        print("Changing type of activity: " + activity.activityId);
                        db.activity.update({activityId: activity.activityId}, {$set: {type: replaceWith}});

                        fixed = true;
                    }
                    else {
                        print("Deleting activity: " + activity.activityId);
                        db.activity.update({activityId: activity.activityId}, {$set: {status: 'deleted'}});
                    }
                }

            }
        }
    }
    else {
        i++;
        while (activities.hasNext()) {
            var activity = activities.next();

            if (activity.progress == 'planned') {
                print("Changing type of activity: " + activity.activityId);
                db.activity.update({activityId: activity.activityId}, {$set: {type: replaceWith}});
            }
            else if (activity.progress == 'started' || activity.progress == 'finished') {
                print("Adding activity as a clone of " + activity.activityId);
                //delete activity._id;
                //activity.activityId = UUID.generate();
                //activity.type = replaceWith;
                //db.activity.insert(activity);
            }

        }
    }


}

print(i+ " simple cases");