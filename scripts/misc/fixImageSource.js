//db.record.find({$and: [{ 'multimedia.documentId': {$ne: null}}, {"multimedia.identifier": /images-dev/}] }).count()

db.record.find(
    {$and: [{ 'multimedia.documentId': {$ne: null}}, {"multimedia.identifier": /images-dev/}] }
).forEach(function(doc) {

    print ("outputSpeciesId: " + doc.outputSpeciesId)

    for (var i=0; i < doc.multimedia.length; i++)
    {
        var mmEntry = doc.multimedia[i]
        print ("Old Identifier: " + mmEntry.identifier)
        print ("ImageId: " + mmEntry.imageId)

        // Array still might have entries that are not pointing to images-dev
        if(mmEntry.identifier && mmEntry.identifier.indexOf("images-dev") >= 0) {
            var documentId = mmEntry.documentId
            if(documentId) {
                db.document.find({documentId:documentId}).forEach(function(docu) {
                    print("New Identifier: " + docu.identifier)

                    // Comment out for a dry run
                    db.record.update({"_id":doc._id, "multimedia.imageId": mmEntry.imageId},
                        {$set:{"multimedia.$.identifier":docu.identifier}} );
                })
            }
        }
    }
} )