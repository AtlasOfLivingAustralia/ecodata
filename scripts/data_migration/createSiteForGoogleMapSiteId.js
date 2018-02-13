load('uuid.js');
var activityQuery = {siteId: "Google maps"};
var numberOfAcitivities = db.activity.find(activityQuery).count();
print("Number of activities to be cleared\t" + numberOfAcitivities);


var activities = db.activity.find(activityQuery);
var counter = 0;
activities.forEach(function (activity) {
    if(activity) {
        var fieldName = 'location',
            activityId = activity.activityId,
            output = db.output.findOne({activityId: activityId, 'data.location': "Google maps"}),
            geoJson = getGeoJson(output, fieldName),
            extent = getExtent(geoJson, output, fieldName);

        var siteId = createSiteForActivity(geoJson, extent, activity, output, fieldName);
        db.activity.update({activityId: activityId}, {$set: {siteId: siteId}});
        db.output.update({activityId: activityId, 'data.location': "Google maps"}, {$set: {"data.location": siteId}});
        counter += getNumberOfUpdatedRecords()
    }
});

print("Number of outputs updated\t" + counter);

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
    print("Created site\t" + site.siteId);
    return site.siteId
}

function getNumberOfUpdatedRecords() {
    var message = db.runCommand("getlasterror");
    if(message.updatedExisting){
        return message.n;
    } else {
        return 0;
    }
}