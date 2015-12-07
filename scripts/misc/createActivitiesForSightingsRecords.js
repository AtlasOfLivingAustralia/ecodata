load('uuid.js');

var sightingsProject = db.project.find({name:'Individual sightings'}).next();
var sightingsSurvey = db.projectActivity.find({projectId:sightingsProject.projectId}).next();

var records = db.record.find({importedFrom:'ecodata-sightings'});
var count = 0;
while (records.hasNext()) {
    var record = records.next();

    var eventDate = record.eventDate;
    var dateStr,timeStr;
    if (eventDate) {
        try {
            eventDate = ISODate(eventDate);
            dateStr = eventDate.getDate() + '-' + eventDate.getMonth() + '-' + eventDate.getFullYear();
            timeStr = eventDate.getHours() + ':' + eventDate.getMinutes();
        }
        catch (e) {
            print("invalid date: "+eventDate);
        }
    }

    var activity = {
        projectId:sightingsProject.projectId,
        projectActivityId:sightingsSurvey.projectActivityId,
        type:'Single Sighting',
        status:'active',
        dateCreated:record.dateCreated,
        lastUpdated:record.lastUpdated,
        userId:record.userId,
        activityId:UUID.generate(),
        importedFrom:'ecodata-sightings'
    };
    db.activity.save(activity);

    var output = {
        activityId:activity.activityId,
        outputId:UUID.generate(),
        importedFrom:'ecodata-sightings',
        name:'Single Sighting',
        status:'active',
        dateCreated:record.dateCreated,
        lastUpdated:record.lastUpdated,
        data: {
            eventDate:record.eventDate,
            multimedia:record.multimedia,
            recordedBy:record.recordedBy,
            coordinateUncertaintyInMeters:record.coordinateUncertaintyInMeters,
            verbatimCoordinates:[record.decimalLongitude, record.decimalLatitude],
            geodeticDatum:record.geodeticDatum,
            georeferenceProtocol:record.georeferenceProtocol,
            identificationVerificationStatus:record.identificationVerificationStatus,
            individualCount:record.individualCount,
            locality:record.locality,
            name:record.scientificName,
            submissionMethod:record.submissionMethod,
            imageLicence:record.imageLicence,
            occurrenceRemarks:record.occurrenceRemarks,
            dateStr:dateStr,
            timeStr:timeStr

        }

    };

    db.output.save(output);

    record.activityId = activity.activityId;
    record.projectActivityId = sightingsSurvey.projectActivityId;
    record.projectId = sightingsProject.projectId;
    record.verbatimCoordinates = [record.decimalLongitude, record.decimalLatitude]
    record.name = record.scientificName

    db.record.save(record);

    count++;
    if (count % 100 == 0) {
        print("Created "+count+" activities");
    }
}