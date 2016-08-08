load('/Users/sat01a/All/sat01a_git/merged/ecodata/scripts/misc/uuid.js');
// cd /Users/sat01a/All/j2ee/mongodb_2.6.2/bin
// ./mongo /Users/sat01a/All/sat01a_git/merged/ecodata-2/scripts/misc/importSightingsDataToEcodata.js
/*
 mr = db.runCommand({
 "mapreduce" : "record",
 "map" : function() {
 for (var key in this) { emit(key, null); }
 },
 "reduce" : function(key, stuff) { return null; },
 "out": "things" + "_keys"
 })

 [
 "_id",
 "commonName",
 "coordinatePrecision",
 "coordinateUncertaintyInMeters",
 "dataResourceUid",
 "dateCreated",
 "decimalLatitude",
 "decimalLongitude",
 "device",
 "devicePlatform",
 "eventDate",
 "family",
 "geodeticDatum",
 "georeferenceProtocol",
 "identificationVerificationStatus",
 "imageLicence",
 "individualCount",
 "kingdom",
 "lastUpdated",
 "locality",
 "locationRemark",
 "multimedia",
 "occurrenceID",
 "occurrenceRemarks",
 "projectId",
 "recordedBy",
 "scientificName",
 "submissionMethod",
 "tags",
 "taxonConceptID",
 "userDisplayName",
 "userId",
 "usingReverseGeocodedLocality",
 "verbatimLatitude",
 "verbatimLongitude"
 ]

 */

// IMPORTANT : Make sure to enable "Allow public users to enter data" flag under survey settings.

var sightingsConn = new Mongo();
var sightingsDb = sightingsConn.getDB("ecodata-sightings");
var ecodataDb = sightingsConn.getDB("ecodata");
print('Total Sightings Record to import : ' + sightingsDb.record.find({}).count());

print('Deleting old sightings import');
ecodataDb.userPermission.remove({importedFrom: "ecodata-sightings"});
ecodataDb.activity.remove({importedFrom: "ecodata-sightings"});
ecodataDb.output.remove({importedFrom: "ecodata-sightings"});
ecodataDb.record.remove({importedFrom: "ecodata-sightings"});
ecodataDb.document.remove({importedFrom: "ecodata-sightings"});
ecodataDb.location.remove({importedFrom: "ecodata-sightings"});

print('Importing sightings data...');
var ecodataProject = ecodataDb.project.find({projectId: '89c78c40-29e5-46f6-8720-d1e3bd2f170a'}).next();
var ecodataSurvey = ecodataDb.projectActivity.find({projectId: ecodataProject.projectId}).next();
var count = 0;
var records = sightingsDb.record.find({});
var formName = "ALA Single Sighting";
var importedFrom = 'ecodata-sightings';

//while (count < 100) {
while (records.hasNext()) {
    var record = records.next();

    // There are around 245 records without common name and scientific name.
    if (!record.scientificName && !record.commonName) {
        record.scientificName = 'unlisted species'
    }

    if (!record.decimalLongitude || !record.decimalLatitude) {
        record.decimalLatitude = -25.7;
        record.decimalLongitude = 134.1;
    }

    //Get userId and see whether we need to create userPermission.
    var userPermission = ecodataDb.userPermission.find({
        $and: [{
            userId: record.userId,
            entityId: ecodataProject.projectId
        }]
    });

    if (userPermission.count() == 0) {
        userPermission = {
            accessLevel: "projectParticipant",
            entityId: ecodataProject.projectId,
            entityType: "au.org.ala.ecodata.Project",
            status: "active",
            userId: record.userId,
            importedFrom: "ecodata-sightings"
        };
        ecodataDb.userPermission.save(userPermission);
    }

    //Create activity
    var eventDate = record.eventDate;
    var dateStr, timeStr;
    if (eventDate) {
        try {
            // 2012-03-11T04:00:00+0000
            var str = record.eventDate.toString();

            var year = str.substr(0, 4);
            var month = str.substr(5, 2);
            var date = str.substr(8, 2);
            var hours = str.substr(11, 2);
            var min = str.substr(14, 2);
            var sec = str.substr(17, 2);

            /*
             ALA Single sightings doesn't use ISO date properly.
             eventDate = ISODate(eventDate);
             var hours = eventDate.getHours();
             var ampm = 'AM';
             if (hours == 12) {
             ampm = 'PM';
             } else if (hours == 0) {
             hours = 12;
             } else if (hours > 12) {
             hours -= 12;
             ampm = 'PM';
             }
             var min = eventDate.getMinutes() <= 9 ? '0'+eventDate.getMinutes() : eventDate.getMinutes();
             hours = hours <= 9 ? '0'+ hours : hours;
             timeStr = hours + ':' + min + " " + ampm;
             if(record.recordedBy == 'Norman Aitken') {
             print(record.eventDate);
             print(timeStr);
             }
             */

            var ampm = 'AM';
            if (hours == 12) {
                ampm = 'PM';
            } else if (hours == 0) {
                hours = 12;
            } else if (hours > 12) {
                hours -= 12;
                ampm = 'PM';
            }
            timeStr = hours + ":" + min + " " + ampm;

        }
        catch (e) {
            print("invalid date: " + eventDate);
        }
    }

    var activity = record.activityId ? ecodataDb.activity.find({activityId: record.activityId}).next() : '';
    if (!activity) {
        activity = {
            type: formName,
            status: 'active',
            dateCreated: record.dateCreated,
            lastUpdated: record.lastUpdated,
            userId: record.userId,
            activityId: UUID.generate(),
            importedFrom: importedFrom
        };
    }

    activity.projectId = ecodataProject.projectId;
    activity.projectActivityId = ecodataSurvey.projectActivityId;
    activity.coordinates = [record.decimalLongitude, record.decimalLatitude];
    activity.assessment = false;
    ecodataDb.activity.save(activity);

    var output = record.activityId ? ecodataDb.output.find({'activityId': record.activityId}).next() : '';
    if (!output) {

        // Map certainity
        var certainOrUncertain = record.identificationVerificationStatus ? record.identificationVerificationStatus : 'Certain';
        if (record.identificationVerificationStatus) {
            if (record.identificationVerificationStatus == 'Confident') {
                certainOrUncertain = 'Certain';
            }
            else if (record.identificationVerificationStatus == 'Uncertain') {
                certainOrUncertain = 'Uncertain';
            } else {
                certainOrUncertain = 'Certain';
            }
        }

        //Used in pasearch indexing
        record.guid = record.occurrenceID;

        // Build species info
        var species = {};
        species.guid = record.occurrenceID;
        species.outputSpeciesId = UUID.generate();
        species.name = record.scientificName;
        var outputId = UUID.generate();

        //Multimedia??
        var sightingPhoto1 = [];
        var multimedia = record.multimedia ? record.multimedia : [];

        var documents = [];
        for (var i = 0; i < multimedia.length; i++) {
            var image = multimedia[i];
            var imageDocument = {};
            var licence = "";
            if (image.license == 'Creative Commons Attribution') {
                licence = "CC BY"
            } else if (image.licence == 'Creative Commons Attribution-Noncommercial') {
                licence = "CC BY-NC"
            } else if (image.licence == 'Creative Commons Attribution-Share Alike') {
                licence = "CC BY-SA"
            } else if (image.licence == 'Creative Commons Attribution-Noncommercial-Share Alike') {
                licence = "CC BY-NC-SA"
            }

            var document = {};
            document.documentId = UUID.generate();
            document.activityId = activity.activityId;
            document.attribution = "";
            document.filename = image.title;
            document.filepath = "";
            document.isPrimaryProjectImage = false;
            document.isSciStarter = false;
            document.labels = [];
            document.licence = licence;
            document.name = image.title;
            document.outputId = outputId;
            document.role = "surveyImage";
            document.status = "active";
            document.thirdPartyConsentDeclarationMade = false;
            document.type = "image";
            document.contentType = image.format;
            document.formattedSize = "";
            document.dateTaken = "";
            document.filesize = "";
            document.notes = "";
            document.identifier = image.identifier;
            document.importedFrom = importedFrom;
            document.hosted = "images.ala.org.au";
            document.imageId = image.imageId; // Used to check whether to upload the image to ala image server,

            imageDocument.filepath = "";
            imageDocument.licence = licence;
            imageDocument.status = "active";
            imageDocument.outputId = outputId;
            imageDocument.contentType = image.format;
            imageDocument.type = "image";
            imageDocument.formattedSize = '';
            imageDocument.activityId = activity.activityId;
            imageDocument.dateTaken = ""; // Not available.
            imageDocument.filesize = "";
            imageDocument.name = image.title;
            imageDocument.role = 'surveyImage';
            imageDocument.notes = "";
            imageDocument.identifier = image.identifier;
            imageDocument.documentId = document.documentId;
            imageDocument.attribution = "";
            sightingPhoto1.push(imageDocument);
            documents.push(document);
        }

        output = {
            activityId: activity.activityId,
            outputId: outputId,
            importedFrom: importedFrom,
            name: formName,
            status: 'active',
            dateCreated: record.dateCreated,
            lastUpdated: record.lastUpdated,
            data: {
                tags: record.tags,
                identificationConfidence1: certainOrUncertain,
                locationNotes: record.locationRemark,
                locationSource: record.georeferenceProtocol,
                surveyStartTime: timeStr,
                locationAccuracy: record.coordinateUncertaintyInMeters,
                locationLongitude: record.decimalLongitude,
                locationLatitude: record.decimalLatitude,
                species1: species,
                comments1: record.occurrenceRemarks,
                recordedBy: record.recordedBy,
                locationLocality: record.locality,
                individualCount1: record.individualCount,
                surveyDate: record.eventDate,
                notes: '',
                sightingPhoto1: sightingPhoto1

            }
        };
    }

    output.activityId = activity.activityId;
    ecodataDb.output.save(output);

    record.activityId = activity.activityId;
    record.projectActivityId = ecodataSurvey.projectActivityId;
    record.projectId = ecodataProject.projectId;
    record.verbatimCoordinates = [record.decimalLongitude, record.decimalLatitude];
    record.name = record.scientificName;
    record.status = 'active';
    record.outputId = output.outputId;
    record.outputSpeciesId = species.outputSpeciesId;
    record.importedFrom = importedFrom;

    ecodataDb.record.save(record);

    for (var i = 0; i < documents.length; i++) {
        var doc = documents[i];
        ecodataDb.document.save(doc);
    }

    count++;

    if (count % 1000 == 0) {
        print("Created " + count + " activities");
    }
}

print('Importing location/bookmark...');
print('Total location/bookmark to import : ' + sightingsDb.location.find({}).count());
// import location bookmark.
var locations = sightingsDb.location.find({});
while (locations.hasNext()) {
    var location = locations.next();
    location.importedFrom = importedFrom;
    location.locationId = UUID.generate();
    ecodataDb.location.save(location);
}

print('Done.');
