
var updateUndefinedToNull = function (collection, id, fieldToUpdate, origValue) {
    print ("the undefined field is " + fieldToUpdate + " : " + origValue);
    print ('updating ' + fieldToUpdate + ' to null');
    var update = {};
    //update[parentfield + field] = null;
    update[fieldToUpdate] = null;
    db.getCollection(collection).update({_id: id}, {$set: update});
};

print ("######## updateEmptyGeoIndex.js");

db.getCollection('site').find({"geoIndex": {$exists: true}, "geoIndex": {}}).forEach(function(doc){
    printjson (doc);
    print ('updating ' + doc['_id'] + ' geoIndex to null');
    updateUndefinedToNull('site', doc['_id'], 'geoIndex', doc['geoIndex']);

});

