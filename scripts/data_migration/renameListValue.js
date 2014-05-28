function renameListValue(outputName, property, oldValue, newValue) {

    var query = {name:outputName};
    query[property] = oldValue;
    var affectedCount = db.output.find(query).count();

    if (affectedCount > 0) {
        var update = {$set:{}};
        update.$set[property] = newValue;
        db.output.update(query, update, false, true);

        query[property] = newValue;
        var updatedCount = db.output.find(query).count();

        if (updatedCount != affectedCount) {
            throw {name:'Error', message:'Updating '+outputName+'.'+property+', expected '+affectedCount+', was '+updatedCount};
        }

        print('Updated '+updatedCount+' outputs. '+outputName+'.'+property+' from '+oldValue+' to '+newValue);
    }
    else {
        print('No outputs with '+outputName+'.'+property+' = '+oldValue);
    }

}

function renameArrayTypedListValue(outputName, property, oldValue, newValue) {

    var query = {name:outputName};
    query[property] = oldValue;
    var affectedCount = db.output.find(query).count();

    if (affectedCount > 0) {

        var update = {$set:{}};
        update.$set[property+'.$'] = newValue;
        db.output.update(query, update, false, true);

        query[property] = newValue;
        var updatedCount = db.output.find(query).count();

        if (updatedCount != affectedCount) {
            throw {name:'Error', message:'Updating '+outputName+'.'+property+', expected '+affectedCount+', was '+updatedCount};
        }

        print('Updated '+updatedCount+' outputs. '+outputName+'.'+property+' from '+oldValue+' to '+newValue);
    }
    else {
        print('No outputs with '+outputName+'.'+property+' = '+oldValue);
    }

}

function renameNestedListValue(outputName, listProperty, property, oldValue, newValue) {
    var query = {name:outputName};
    query[listProperty+'.'+property] = oldValue;
    var affectedCount = db.output.find(query).count();

    var updatedCount = 0;
    if (affectedCount > 0) {

        var outputs = db.output.find(query);

        while (outputs.hasNext()) {
            var found = false;
            var output = outputs.next();
            var list = output[listProperty];
            for (var i=0; i<list.length; i++) {
                if (output[listProperty][i] === oldValue) {
                    output[listProperty][i] === newValue;
                    found = true;
                }
            }
            if (!found) {
                throw {name:'Error', message:'Updating '+outputName+'.'+listProperty+'.'+property+', expected match in '+tojson(output)};
            }
            updatedCount++;
            db.output.save(output);
        }

        if (updatedCount != affectedCount) {
            throw {name:'Error', message:'Updating '+outputName+'.'+property+', expected '+affectedCount+', was '+updatedCount};
        }

        print('Updated '+updatedCount+' outputs. '+outputName+'.'+property+' from '+oldValue+' to '+newValue);
    }
    else {
        print('No outputs with '+outputName+'.'+listProperty+'.'+property+' = '+oldValue);
    }
}
