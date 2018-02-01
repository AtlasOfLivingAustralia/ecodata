//Number of hubs changed
var hubCount = db.hub.find({}).count();
print( "Number of hub facets migrated\t" + hubCount );

var countSitesTrue = db.projectActivity.find({allowAdditionalSurveySites:true}).count();
var countSitesFalse = db.projectActivity.find({$or:[{allowAdditionalSurveySites:{$exists:false}}, {allowAdditionalSurveySites:false}]}).count();
print( "Number of configuration to be updated\t" + ( countSitesTrue + countSitesFalse ) );

// Clearing stored sites to show user selected point data
var projects =  db.runCommand({
        "distinct" :  "project",
        "key" : "projectId",
        "query" :  {
            "isMERIT": false
        }
    }).values;
var activites = db.runCommand({
        "distinct" :  "activity",
        "key" : "activityId",
        "query" :  {projectId : { $in: projects}}
    }).values;
var countOutputLocationsToClear = db.output.find({activityId: {$in: activites}, 'data.locationLatitude':{$ne:null}, 'data.locationLongitude':{$ne:null}, 'data.location':{$ne:null}}).count();
print( "Clearing stored sites to show user selected point data\t" + countOutputLocationsToClear );


// Number of sites to be created from point coordinates
var query = {
    "data.locationLatitude" : {$exists:true},
    "data.locationLongitude": {$exists:true},
    "data.location" : null,
    "activityId": {$in: activites}
};
var countPrivateSitesCreated = db.output.find(query).count();
print( "Number of private sites to be created\t" + (countPrivateSitesCreated + countOutputLocationsToClear));

// Number of activities with siteId updated
query = {
    'siteId' : { $in : ["", null]},
    'activityId': {$in: activites}
};
var activitiesWithoutSiteId = db.runCommand({
    "distinct" :  "activity",
    "key" : "activityId",
    "query" :  query
}).values;
var countSiteIdUpdated = db.output.find({activityId: {$in: activitiesWithoutSiteId}, "data.location": { $nin: ["", null, "null"]}}).count();
print( "Number of activities updated with sites\t" + (countSiteIdUpdated - countOutputLocationsToClear) );
