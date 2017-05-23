//load('uuid.js');
var projects = db.project.find({associatedSubProgram:'Regional Funding', status:{$ne:'deleted'}});
var organisations = [];
var organisationNames = [
        'Central Tablelands Local Land Services',
        'Central West Local Land Services',
        'Greater Sydney Local Land Services',
        'Murray Local Land Services',
        'Burnett Mary Regional Group For Natural Resource Management Ltd',
        'Fitzroy Basin Association Inc.',
        'Kangaroo Island Natural Resources Management Board',
        'Northern and Yorke Natural Resources Management Board',
        'South East Natural Resources Management Board',
        'Mallee Catchment Management Authority',
        'West Gippsland CMA',
        'Glenelg - Hopkins Catchment Management Authority',
        'Perth Region NRM',
        'South Coast Natural Resource Management Inc.',
        'Rangelands NRM Co-ordinating Group (Inc.)',
        'South Australian Murray-Darling Basin Natural Resources Management Board',
        'Goulburn Broken Catchment Management Authority',
        'Adelaide & Mt Lofty Ranges NRM Board',
        'FNQ NRM Ltd'
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
    var toReplace = 'Stage Report';
    var replaceWith = 'Annual Stage Report';
    while (projects.hasNext()) {
        var project = projects.next();

        if (organisations.indexOf(project.organisationId) >= 0) {
            print("Matched project "+project.projectId+" for org "+project.organisationName);
            print("Updating planned progress reports for project: "+project.projectId);
            var activities = db.activity.find(
                {projectId:project.projectId,
                    plannedEndDate:{$gt:ISODate("2017-06-01T00:00:00Z"), $lt:ISODate("2017-08-01T00:00:00Z")},
                    type:toReplace,
                    status:{$ne:'deleted'}});

            while (activities.hasNext()) {
                var activity = activities.next();
                if (activity.progress == 'planned' || activity.progress == 'started') {
                    print("Changing type of activity: "+activity.activityId);
                    db.activity.update({activityId:activity.activityId}, {$set:{type:replaceWith}});
                }
                else if (activity.progress == 'started') {
                    print("Adding activity as a clone of "+activity.activityId);
                    //delete activity._id;
                    //activity.activityId = UUID.generate();
                    //activity.type = replaceWith;
                    //db.activity.insert(activity);
                }

            }

            var activities = db.activity.find(
                {projectId:project.projectId,
                    plannedEndDate:{$gt:ISODate("2017-06-01T00:00:00Z"), $lt:ISODate("2017-08-01T00:00:00Z")},
                    type:'Progress, Outcomes and Learning - stage report',
                    status:{$ne:'deleted'}});

            while (activities.hasNext()) {
                var activity = activities.next();
                if (activity.progress == 'planned') {
                    print("Changing type of activity: "+activity.activityId);
                    db.activity.update({activityId:activity.activityId}, {$set:{type:replaceWith}});
                }
                else if (activity.progress == 'started') {
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
