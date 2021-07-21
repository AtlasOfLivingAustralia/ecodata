
var recursiveSearch = function(collection) {

    var updateUndefinedToNull = function (collection, id, fieldToUpdate, origValue) {
        print ("the undefined field is " + fieldToUpdate + " : " + origValue);
        print ('updating ' + fieldToUpdate + ' to null');
        var update = {};
        //update[parentfield + field] = null;
        update[fieldToUpdate] = null;
        db.getCollection(collection).update({_id: id}, {$set: update});
    }

    var deepIterate = function (collection, id, obj, parent) {
        var toPrintObj = false;
        var parentfield = parent != '' ? parent + '.' : '';

        for (var field in obj) {

            if ((obj[field] == undefined) || (obj[field] == "null" && typeof obj[field] == 'string')) {
                toPrintObj = true;
                updateUndefinedToNull(collection, id, parentfield + field, obj[field]);
             //   print(typeof obj[field])
             //   print ("the undefined field is " + parentfield + field + " : " + obj[field]);
                continue;
            }
            var found = false;
            if (typeof obj[field] == 'object') {
                found = deepIterate(collection, id, obj[field], parentfield + field)
                if (found) {
                    toPrintObj = true;
                    continue;
                }
            }

        }

        if (toPrintObj) {
            return true;
        }

        return false;

    };

    // db.getCollection(collection).find({siteId:  'a805e770-10cd-4ead-ad4e-d13d9360fb38'}).forEach(function(doc){
    db.getCollection(collection).find().forEach(function(doc){
        print("Scanning document with Id: " + doc['_id']);
        var docContainUndefined = deepIterate(collection, doc['_id'], doc, "");
        if (docContainUndefined) {
            //printjson (doc);
            print("updated id: "+ doc['_id']);
        } else {
            print ('Nothing to update for id: ' + doc['_id']);
        }
    });
}

print ("######## updateUndefinedValues.js ##### Update Output collection ########" );
recursiveSearch('output');

print ("######## updateUndefinedValues.js ##### Update Site collection ########" );
recursiveSearch('site');

print ("######## updateUndefinedValues.js ##### Update Hub collection ########" );
recursiveSearch('hub');

print ("######## updateUndefinedValues.js ##### Update Score collection ########" );
recursiveSearch('score');

print ("######## updateUndefinedValues.js ##### Update Record collection ########" );
recursiveSearch('record');
