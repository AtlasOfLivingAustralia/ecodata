//
//  This script updates the record multimedia with document id based on the image document imageid
//  This is used in downloadService where it retrieve the images for the records and then gets the document related to the multimedia
//
// Run this file from shell like this:
//       mongo ecodata updateRecordDocumentId.js > output.txt
// Check the number of records updated. Number of records updated should match the records retrieved


print("Calling updateRecordDocument.js");

function printObject(o) {

    var out = '';

    for (var p in o) {
        var count = 0;
        out += '"' + p.record;
        for (var i in p.multimedia) {
            if (count == 0) {
                out += '", "'
            }
            count ++;
            out += i + " ";
        }
        out += '"' + '\n';
    }
    print(out);
}


// Find the records that have multimedia but without document id
var records = db.record.find({ $and : [{'multimedia': { $exists: true } }, { 'multimedia.documentId': { $exists: false } }, {'multimedia.type': { $exists: true } }, {'multimedia.imageId': { $exists: true } } ]});

print ("Retrieved records: " + records.count());

var updateRecCount = 0
var recordListMap = [];
var noMultimedia = [];

while (records.hasNext()) {

    var updateMultimediaCount = 0;
    var noDocMultimedia = [];
    var updatedImageId = [];

    var record = records.next();
    var activityId = record.activityId;

  //  print("Retrieved record with activityId: "+activityId);

    var multimedia = record.multimedia;
    if (multimedia && multimedia.length > 0) {
        for (var i = 0; i < multimedia.length; i++) {
            var image = multimedia[i];

            var documents = db.document.find({'activityId': activityId });

            while (documents.hasNext()) {
                var imageDocument = documents.next();
                if (imageDocument.imageId == image.imageId) {
                    image.documentId = imageDocument.documentId;
                //    print("Updating documentId to "+imageDocument.documentId + " for record: " + record._id + " imageId: " +  image.imageId);
                    updateMultimediaCount ++;
                    updatedImageId.push(image.imageId);
                }
            }
        }

        if (updateMultimediaCount > 0) {
            if (updateMultimediaCount == multimedia.length) {
                print("Updating record with id: " + record._id);
                db.record.save(record);
                updateRecCount ++;
            }
        }

        // In case some multimedia don't have documents, we want to list these problematic ones
        if (updateMultimediaCount < multimedia.length) {
            print("Records without doc: " + record._id + "\n");

            noDocMultimedia = multimedia.filter(function(x) {
                return (updatedImageId.indexOf(x.imageId) < 0);
            });
            if (noDocMultimedia.length > 0) {
                var obj = {'record': record._id, 'multimedia': noDocMultimedia}
                recordListMap.push(obj);
            }
        }

    } else {
        // list records without multimedia
        noMultimedia.push(record._id)
    }

  //  break;

}

print('\nTotal records updated: ' + updateRecCount + '\n');

if (recordListMap.length > 0) {
    print('\nMultimedia records without document:\n');
    printObject (recordListMap);
}

if (noMultimedia.length > 0) {
    print('\n\nRecords with no multimedia:\n');
    var out = '';

    for (var p in noMultimedia) {
        out += '"' + p +'"' + '\n';
    }
    print(out);
}