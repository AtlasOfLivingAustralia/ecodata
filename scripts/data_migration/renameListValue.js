function renameListValue(outputName, property, oldValue, newValue) {
    property = 'data.'+property;
    var query = {name:outputName};
    query[property] = oldValue;
    var affectedCount = db.output.find(query).count();

    if (affectedCount > 0) {
        var update = {$set:{}};
        update.$set[property] = newValue;
        db.output.update(query, update, false, true);

        query[property] = newValue;
        var updatedCount = db.output.find(query).count();

        if (updatedCount < affectedCount) {
            throw ('Updating '+outputName+'.'+property+', expected '+affectedCount+', was '+updatedCount);
        }

        print('Updated '+updatedCount+' outputs. '+outputName+'.'+property+' from '+oldValue+' to '+newValue);
    }
    else {
        print('No outputs with '+outputName+'.'+property+' = '+oldValue);
    }

}

function renameArrayTypedListValue(outputName, property, oldValue, newValue) {

    var nestedProperty = property;
    property = 'data.'+property;
    var query = {name:outputName};
    query[property] = oldValue;
    var affectedCount = db.output.find(query).count();
    query[property] = newValue;

    if (affectedCount > 0) {

        query[property] = oldValue;
        var outputs = db.output.find(query);
        var count = 0;
        while (outputs.hasNext()) {
            var output = outputs.next();
            var prop = output.data[nestedProperty];
            var newProp = [];
            for (var i=0; i<prop.length; i++) {
                if (prop[i] != oldValue) {
                    newProp.push(prop[i]);
                }
                if (newProp.indexOf(newValue) < 0) {
                    newProp.push(newValue);
                }
            }
            output.data[nestedProperty] = newProp;
            db.output.save(output);
            count ++;
        }

        query[property] = oldValue;
        var stillAffectedCount = db.output.find(query).count();


        if (stillAffectedCount > 0) {
            print('Error updating '+outputName+'.'+property+', expected '+affectedCount+', still affected: '+stillAffectedCount);
            throw {name:'Error', message:'Updating '+outputName+'.'+property+', expected '+affectedCount+', was '+count};
        }

        print('Updated '+count+' outputs. '+outputName+'.'+property+' from '+oldValue+' to '+newValue);
    }
    else {
        print('No outputs with '+outputName+'.'+property+' = '+oldValue);
    }

}

// Do not include 'data.' prefix on the listProperty parameter.
function renameNestedListValue(outputName, listProperty, property, oldValue, newValue) {
    var query = {name:outputName};
    query['data.'+listProperty+'.'+property] = oldValue;
    var affectedCount = db.output.find(query).count();

    var updatedCount = 0;
    if (affectedCount > 0) {

        var outputs = db.output.find(query);

        while (outputs.hasNext()) {
            var found = false;
            var output = outputs.next();

            var list = output.data[listProperty];

            for (var i=0; i<list.length; i++) {
                if (output.data[listProperty][i][property] === oldValue) {
                    output.data[listProperty][i][property] = newValue;
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

        print('Updated '+updatedCount+' outputs. '+outputName+' '+listProperty+'.'+property+' from "'+oldValue+'" to "'+newValue+'"');
    }
    else {
        print('No outputs with '+outputName+'.'+listProperty+'.'+property+' = '+oldValue);
    }
}
