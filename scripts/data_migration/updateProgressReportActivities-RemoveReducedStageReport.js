//load('uuid.js');
var projects = db.project.find({associatedSubProgram:'Regional Funding', status:{$ne:'deleted'}});
var organisations = [];
var organisationNames = [
    'Corangamite Catchment Management Authority',
    'ACT',
    'Port Phillip and Westernport Catchment Management Authority',
    'Perth Region NRM',
    'Western Local Land Services',
    'Northern Tablelands Local Land Services',
    'South Australian Murray-Darling Basin Natural Resources Management Board',
    'Mallee Catchment Management Authority',
    'South Coast Natural Resource Management Inc.',
    'West Gippsland CMA'

    ];

var orgsuccess = true;
for (var i=0; i<organisationNames.length; i++) {
    var organisation = db.organisation.find({name:organisationNames[i]});
    if (organisation.hasNext()) {
        organisations.push(organisation.next().organisationId);
    }
    else {
        orgsuccess = false;
        print("No organisation found for name: "+organisationNames[i]);
    }
}

if (orgsuccess) {
    var toReplace = ['Annual Stage Report', 'Progress, Outcomes and Learning - stage report'];
    var replaceWith = 'Stage Report';
    while (projects.hasNext()) {
        var project = projects.next();

        if (organisations.indexOf(project.organisationId) >= 0) {
            print("Matched project "+project.projectId+" for org "+project.organisationName);
            var activities = db.activity.find(
                {projectId:project.projectId,
                    //plannedEndDate:{$gt:ISODate("2017-06-01T00:00:00Z"), $lt:ISODate("2017-08-01T00:00:00Z")},
                    plannedEndDate:{$gt:ISODate("2017-08-01T00:00:00Z")},
                    type:{$in:toReplace},
                    status:{$ne:'deleted'}});

            if (activities.count() == 0) {
                print("No matching activities for project "+project.projectId)
            }
            while (activities.hasNext()) {
                var activity = activities.next();

                if (activity.progress == 'planned') {
                    print("Changing type of activity: "+activity.activityId);
                    db.activity.update({activityId:activity.activityId}, {$set:{type:replaceWith}});
                }
                else if (activity.progress == 'started' || activity.progress == 'finished') {
                    print("Adding activity as a clone of "+activity.activityId);
                    //delete activity._id;
                    //activity.activityId = UUID.generate();
                    //activity.type = replaceWith;
                    //db.activity.insert(activity);
                }

            }

        }
        else if (organisationNames.indexOf(project.organisationName) >= 0) {
            print("Project "+project.projectId+" has org name of "+project.organisationName+" but organisation id: "+project.organisationId);
        }

    }
}
