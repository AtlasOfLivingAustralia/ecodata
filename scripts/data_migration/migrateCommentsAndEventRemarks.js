print("Number of outputs with notes not empty - " + db.getCollection('output').find({"data.notes": {$ne: ""}, name: "Single Sighting - Advanced", importedFrom: "ecodata-sightings"}).count());
var outputCount = db.getCollection('output').find({name: "Single Sighting - Advanced", importedFrom: "ecodata-sightings"}).count();
print("Number of outputs that need to be migrated i.e. from comments to notes field. " + outputCount);
// For records imported from sightings, copy comments to notes field.
var outputs = db.getCollection('output').find({name: "Single Sighting - Advanced", importedFrom: "ecodata-sightings"});
var outputCounter = 0;
while (outputs.hasNext()) {
    var output = outputs.next();
    var remarks = output.data.comments;
    if (!output.data.notes && remarks) {
        output.data.notes = remarks;
        output.data.comments = undefined;
        db.output.save(output);
        outputCounter ++;
    }
}
print( "Number of records updated " + outputCounter);
print("Number of outputs with notes not empty - " + db.getCollection('output').find({"data.notes": {$ne: ""}, name: "Single Sighting - Advanced", importedFrom: "ecodata-sightings"}).count());

// For records not imported from sightings,
// migrate record's in eventRemarks field to occurrenceRemarks
var activityIds = db.runCommand({
    "distinct" :  "output",
    "key" : "activityId",
    "query" : {name: "Single Sighting - Advanced", importedFrom: { $ne: "ecodata-sightings" } }
}).values;

var records = db.record.find({activityId: {$in: activityIds}});
print("Number of records to migrate - " + records.count());
var recordCounter = 0;
while (records.hasNext()) {
    var record = records.next();
    if(!record.occurrenceRemarks && record.eventRemarks){
        record.occurrenceRemarks = record.eventRemarks;
        record.eventRemarks = undefined;
        db.record.save(record);
        recordCounter ++;
    }
}

print( "Number of records updated " + recordCounter);