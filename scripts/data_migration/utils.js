function ActivitiesModelHelperFunctions(args){
    args = args || {};
    var cache = {};
    var activitiesModel,
        activityModelLocation = args.activityModelLocation || '/data/ecodata/models/activities-model.json',
        modelLocation = args.modelLocation || '/data/ecodata/models/';

    function getActivitiesModel(){
        if(!activitiesModel){
            var text = cat(activityModelLocation);
            activitiesModel = JSON.parse(text);
        }

        return activitiesModel;
    }

    function getFieldsForActivity (activityName, dataType) {
        var activity = getActivity(activityName);
        if(activity){
            var dataModels = getDataModelsFromActivity(activity);
            var fields = getDataTypeFromDataModels(dataType, dataModels);
            if(fields.length > 1){
                print(activityName + ' has more than one field.');
                print(JSON.stringify(fields, true));
            }

            return fields;
        }
    };

    function getGeoMapFieldsForActivity(activityName) {
        // print('Activity name '+ activityName);
        var dataType = 'geoMap';
        return getFieldsForActivity(activityName, dataType);
    }

    function getActivity(activityName) {
        var activitiesModel = getActivitiesModel();
        var activity;
        for(var index = 0; (index < activitiesModel.activities.length) && !activity; index++){
            var model = activitiesModel.activities[index];
            if(model.name == activityName){
                activity = model;
            }
        }

        return activity;
    }

    function getDataModelsFromActivity(activity) {
        var models = [];
        if(activity.outputs){
            activity.outputs.forEach(function (output) {
                var output = findOutputFromName(output);
                if(output){
                    // print('Template name '+ output.template);
                    models.push(getDataModel(output.template));
                }
            })
        }

        return models;
    }

    function findOutputFromName(outputName) {
        // print(outputName);
        var activitiesModel = getActivitiesModel();
        var outputs = activitiesModel.outputs;
        var result;
        for(var index = 0; (index < outputs.length) && !result; index++){
            var output = outputs[index];
            if(output.name == outputName){
                result = output;
            }
        }

        return result;
    }

    function getDataModel(name) {
        if(!cache[name]){
            var location = modelLocation + name + '/dataModel.json';
            try {
                var text = cat(location);
                cache[name] = JSON.parse(text);
            } catch (err){
                print('file not found - ' + location);
            }
        }

        return cache[name];
    }

    function getDataTypeFromDataModels(dataType, models) {
        models = models || [];
        var results = [];
        models.forEach(function (model) {
            var fields = getDataTypeFromModel(dataType, model);
            Array.prototype.push.apply(results, fields);
        });

        return results;
    }

    function getDataTypeFromModel(dataType, model) {
        var fields = [];
        if(model && model.dataModel){
            model.dataModel.forEach(function (field) {
                if(field.dataType == dataType){
                    fields.push(field);
                }
            })
        }

        return fields;
    }

    return {
        getActivitiesModel: getActivitiesModel,
        getFieldsForActivity: getFieldsForActivity,
        getGeoMapFieldsForActivity: getGeoMapFieldsForActivity,
        getActivity: getActivity,
        getDataModelsFromActivity: getDataModelsFromActivity,
        findOutputFromName: findOutputFromName,
        getDataModel: getDataModel,
        getDataTypeFromDataModels: getDataTypeFromDataModels,
        getDataTypeFromModel: getDataTypeFromModel
    };
}
