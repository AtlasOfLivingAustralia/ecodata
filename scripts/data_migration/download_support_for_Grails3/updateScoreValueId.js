var recursiveSearch = function(collection) {

    var updateDataTypeId = function (collection, id, doc) {
        print ('updating doc with _id: ' + id + ' to objectId')
        db.getCollection(collection).remove({_id: id},true);
        doc._id = new ObjectId();
        db.getCollection(collection).save(doc);
    };


    var iterateDoc = function (collection, id, obj) {
        var toPrintObj = false;

        for (var field in obj) {
            if (field == '_id') {
                print ("the field is " + field + " : " + obj[field] + " typeof " + typeof obj[field]);
                updateDataTypeId(collection, id, obj);
                toPrintObj = true;
                continue;
            }

        }

        if (toPrintObj) {
            return true;
        }

        return false;
    };

    db.getCollection(collection).find().forEach(function(doc){
        print("Scanning document with Id: " + doc['_id']);
        var docContainUndefined = iterateDoc(collection, doc['_id'], doc)
        if (docContainUndefined) {
            printjson (doc);
        } else {
            print ('Nothing to update for document with id: ' + doc['_id']);
        }

    });
};

print ("######## updateScoreValueId.js");
recursiveSearch('score');
