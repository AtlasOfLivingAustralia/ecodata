/**
 * Only for migrating the old data to the new site implementation
 * Qifeng Bai
 * Modified by: Temi
 */
//1, This script is used to set two new fields: allowPolygons and allowPoints to the existing records
// For the existing records which allowAddtionalSurverySites is true, the set allowPolygons to true and allowPoints to true

//2, update projectActivities which allowAddtionalSurveySites does not exist.

function updateAllowPolygonsForProjectActivity() {
    print("------------------");
    print('Update allowPolygons  to false, and allowPoints to true, if allowAdditionalSurveySites is not defined/false');
    print("------------------");
    var cPA_N = db.projectActivity.find({$or: [{allowAdditionalSurveySites: {$exists: false}}, {allowAdditionalSurveySites: false}]});
    cPA_N.forEach(function (pa) {
        var project = db.project.findOne({projectId: pa.projectId});
        print(pa.name + "\t" + pa.projectActivityId + "\t" + project.name + "\t" + project.projectId);
    });


    print("------------------");
    print('Update allowPolygons and allowPoints to true, if allowAdditionalSurveySites is true');
    print("------------------");
    var cPA_N = db.projectActivity.find({allowAdditionalSurveySites: true});
    cPA_N.forEach(function (pa) {
        var project = db.project.findOne({projectId: pa.projectId});
        print(pa.name + "\t" + pa.projectActivityId + "\t" + project.name + "\t" + project.projectId);
    });

    db.projectActivity.update({$or: [{allowAdditionalSurveySites: {$exists: false}}, {allowAdditionalSurveySites: false}]}, {
        $set: {
            allowPolygons: false,
            allowPoints: true,
            allowAdditionalSurveySites: false
        }
    }, {multi: true});
    var counterFalse = getNumberOfUpdatedRecords();

    db.projectActivity.update({allowAdditionalSurveySites: true}, {
        $set: {
            allowPolygons: true,
            allowPoints: true
        }
    }, {multi: true});
    var counterTrue = getNumberOfUpdatedRecords();
    print("Number of Project Activities updated\t" + (counterFalse + counterTrue));
}

/**
 *  Find outputs which have both lng/lat and location in BioCollect project
 *  Log them and remove the location
 */
function updateOutputsWithTwoLocations() {
    print("------------------");
    print('Clearing siteId in the following projects');
    print("------------------");
    var projects =  db.project.find({isMERIT:false})

    var total = 0;
    projects.forEach(function (project) {
        var projectId = project.projectId;
        var activities = db.runCommand({
            "distinct" :  "activity",
            "key" : "activityId",
            "query" :  {projectId : projectId}
        }).values;

        var outputsInproject = 0;
        db.output.update({$and: [{activityId: { $in: activities}}, {'data.locationLatitude': {$ne: null}}, {'data.locationLongitude': {$ne: null}}, {'data.location': {$ne: null}}]}, {$set: {'data.location': null}}, {multi: true});
        var oc = getNumberOfUpdatedRecords();
        total += oc;
        outputsInproject += oc;
        if (outputsInproject > 0) {
            print(project.name  +'\t' +"https://biocollect.ala.org.au/project/index/" + projectId + '\t' + outputsInproject);
        }
    });

    print("Total number of siteId cleared from outputs\t" + total);
}

function getNumberOfUpdatedRecords() {
    var message = db.runCommand("getlasterror");
    if(message.updatedExisting){
        return message.n;
    } else {
        return 0;
    }
}

updateAllowPolygonsForProjectActivity();
updateOutputsWithTwoLocations();
print('Excution is done');