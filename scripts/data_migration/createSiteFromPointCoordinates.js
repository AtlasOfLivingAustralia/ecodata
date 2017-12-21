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
 * Created by Temi on 23/11/17.
 */
load('uuid.js');
load('utils.js');

/**
 * Convert point co-ordinates to site.
 */
function createSiteFromPointCoordinates() {
    var projects = db.runCommand({
        "distinct" :  "project",
        "key" : "projectId",
        "query" :  {
            "isMERIT": false
        }
    }).values;

    var counter = 0,
        sitesCreated = 0;
    var helperFunctions = new ActivitiesModelHelperFunctions();

    var groups = db.activity.aggregate(
        [
            { $match: {projectId: {$in: projects}}},
            { $group : { "_id" : "$type", activities: { $push: "$activityId" } } }
        ]
    );

    groups.result.forEach(function(group){
        var activityModelName = group['_id'],
            query = {activityId: {$in: group.activities}},
            lookup = {};

        var activities = db.activity.find(query);
        while(activities.hasNext()){
            var activity = activities.next();
            lookup[activity.activityId] = activity;
        }

        var fields = helperFunctions.getGeoMapFieldsForActivity(activityModelName);
        if(fields){
            fields.forEach(function (field) {
                var dataField = 'data.' + field.name,
                    latField = field.name + 'Latitude',
                    lonField = field.name + 'Longitude';
                query["data." + lonField] = {$exists:true};
                query["data." + latField] = {$exists:true};
                var outputs = db.output.find(query);

                while(outputs.hasNext()){
                    var output = outputs.next();
                    var data = output.data;
                    if (data && !data[field.name]) {
                        var activityId = output.activityId,
                            activity = lookup[activityId],
                            geoJson = getGeoJson(output, field.name),
                            extent = getExtent(geoJson, output, field.name);

                        if(activity){
                            var siteId = createSiteForActivity(geoJson, extent, activity, output, field.name);
                            counter ++;
                            if(siteId){
                                var outputUpdate = {};
                                outputUpdate[dataField]  = siteId;
                                db.output.update({outputId: output.outputId}, {"$set": outputUpdate});
                                db.activity.update({activityId: activityId}, {"$set": {siteId: siteId}});
                                sitesCreated ++;
                            }
                        } else {
                            print("Could not find activity for id -" + activityId);
                        }
                    }
                }
            });
        }
    });

    print('Total activities checked ' + counter);
    print('Total sites created ' + sitesCreated);
}

function getGeoJson(output, field) {
    var longitudeName = field + 'Longitude',
        latitudeName = field + 'Latitude',
        point = [output.data[longitudeName], output.data[latitudeName]],
        geoJson = {
        "coordinates" : point,
        "type" : "Point"
    };

    return geoJson;
}

function getExtent(geoJson, output, field) {
    var sourceName = field + 'Source',
        source = output.data[sourceName],
        localityName = field + 'Locality',
        locality = output.data[localityName],
        accuracyName = field + 'Accuracy',
        accuracy = output.data[accuracyName],
        point = geoJson.coordinates,
        extent = {
        geometry: {
            type: geoJson.type,
            coordinates: point,
            centre: point,
            decimalLongitude: point[0],
            decimalLatitude: point[1],
            locality: locality,
            source: source,
            uncertainty: accuracy
        },
        source: geoJson.type
    };

    return extent;
}

function createSiteForActivity(geoJson, extent, act, output, field) {
    var noteName = field + 'Notes',
        notes = output.data[noteName],
        name = 'Private site for activity ' + act.activityId,
        site = {
        siteId: UUID.generate(),
        visibility: 'private',
        name: name,
        description: 'Site created when migrating point data in output collection. Data migration - create site for point data. Site created by script createSiteFromPointCoordinates.js.',
        notes: notes,
        dateCreated: act.dateCreated,
        lastUpdated: act.lastUpdated,
        status: 'active',
        projects: [ act.projectId ],
        type: null,
        habitat: null,
        area: 0,
        recordingMethod: null,
        landTenure: null,
        protectionMechanism: null,
        isSciStarter: false,
        extent: extent,
        geoIndex: geoJson
    };

    var result = db.site.insert(site);
    print("Created site - " + site.siteId);
    return site.siteId
}

createSiteFromPointCoordinates();