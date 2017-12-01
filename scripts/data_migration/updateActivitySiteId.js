/*
 * Copyright (C) 2017 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Created by Temi on 1/12/17.
 */
load('utils.js');
function updateActivitySiteId() {
    // get biocollect projects only
    var projects = db.runCommand({
        "distinct" :  "project",
        "key" : "projectId",
        "query" :  {
            "isMERIT": false,
            "status": { '$ne': 'deleted'}
        }
    }).values;

    var activityCounter = 0, notEqual = 0, noSite = 0, activityNeedingUpdate = 0, error = 0, equal = 0;
    var helperFunctions = new ActivitiesModelHelperFunctions();

    // group acitivities by model name
    var groups = db.activity.aggregate(
        [
            { $match: {projectId: {$in: projects}}},
            { $group : { "_id" : "$type", activities: { $push: "$activityId" } } }
        ]
    );

    groups.forEach(function(group) {
        var activityModelName = group['_id'],
            query = {activityId: {$in: group.activities}},
            // dictionary of activities
            lookup = {};

        // get all activities that uses same model. This is done to improve performance.
        var activities = db.activity.find(query);
        while(activities.hasNext()){
            activityCounter ++;
            var activity = activities.next();
            lookup[activity.activityId] = activity;
        }

        // get data model definition for geoMap.
        var fields = helperFunctions.getGeoMapFieldsForActivity(activityModelName);
        if(fields){
            fields.forEach(function (field) {
                var dataField = 'data.' + field.name;
                query[dataField] = {$exists: true};
                // get all outputs where geoMap field exists.
                var outputs = db.output.find(query);

                while (outputs.hasNext()) {
                    var output = outputs.next();
                    if (output.data) {
                        var activityId = output.activityId,
                            activity = lookup[activityId],
                            outputSiteId = output.data[field.name],
                            activitySiteId = activity.siteId;

                        if(!activitySiteId){
                            if(outputSiteId){
                                var cursor = db.site.find({siteId: outputSiteId});
                                if(cursor.hasNext()){
                                    db.activity.update({activityId: activityId}, {"$set": {siteId: outputSiteId}});
                                    activityNeedingUpdate ++
                                } else {
                                    print("Ignoring "+ outputSiteId + " site does not exist.");
                                }
                            } else {
                                error ++;
                            }
                        } else if(outputSiteId && (activitySiteId != outputSiteId)){
                            print('Ignoring activity ' + activityId + '. Activity site id and output site id are not the same. ' + activitySiteId + " " + outputSiteId);
                            notEqual ++;
                        } else if((activitySiteId == outputSiteId)){
                            equal ++;
                        } else {
                            noSite ++;
                        }
                    }
                }
            })
        }
    });

    print('Total number of activities ' + activityCounter);
    var totalOutputs = noSite + equal + notEqual + activityNeedingUpdate + error;
    print('Total number of outputs '+ totalOutputs);
    print('The number of activities with site id updated ' + activityNeedingUpdate);
    print('The number of activities where site id is not equal to output site id ' + notEqual);
    print('The number of activities that need not change ' + equal);
    print('No sites defined ' + noSite);
    print('The number of activities and outputs with no site id ' + error);
}

updateActivitySiteId();