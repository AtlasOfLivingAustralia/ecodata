var documentRecords = db.getCollection('document').find( {$and: [{dateTaken: {$exists: true}}, {dateTaken: {$type: 'string'}} ] });
while (documentRecords.hasNext()) {
    var document = documentRecords.next();

    document.dateTaken = new Date(document.dateTaken);
    db.document.save(document);
}