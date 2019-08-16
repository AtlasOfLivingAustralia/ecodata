/**
 * View model for editing Score objects.
 */
var ScoreModel = function (score, config) {
    var self = this;
    self.label = ko.observable(score.label);
    self.description = ko.observable(score.description);
    self.category = ko.observable(score.category);
    self.units = ko.observable(score.units);
    self.outputType = ko.observable(score.outputType);
    self.externalId = ko.observable(score.externalId);
    self.entityTypes = ko.observableArray(score.entityTypes);
    self.displayType = ko.observable(score.displayType);
    self.isOutputTarget = ko.observable(score.isOutputTarget);
    self.entity = ko.observable(score.entity || 'Activity');

    var editorPane = document.getElementById(config.scoreEditorId);

    function renderFilter(filter) {
        return filter.type + "(" + filter.property+'='+filter.filterValue+")";
    }
    function nodeName(path) {
        var result = editor.getNodesByRange(path);
        var value = result[0].value;
        if (value.filter && value.filter.type) {
            return renderFilter(value.filter);
        }
        else if (value.type == 'filter') {
            return renderFilter(value);
        }
        else if (value.type) {
            return value.type + "(" +value.property+")";
        }
    }

    var options = {
        mode: 'code',
        modes: ['code', 'tree'],
        onNodeName: nodeName
    };
    editor = new JSONEditor(editorPane, options);
    editor.set(score.configuration);

    window.editor = editor;

    self.save = function () {
        var model = ko.toJS(self);

        try {
            model.configuration = editor.get();
            delete model.configurationText;
            var data = JSON.stringify(model);

            delete model.transients;
            $.ajax(config.updateScoreUrl, {
                type: 'POST',
                data: data,
                dataType: 'json',
                contentType: 'application/json',
                success: function (data) {
                    if (data !== 'error') {
                        alert('saved');
                        document.location.href = editScoreUrl + '/' + data.scoreId + '.json';
                    } else {
                        alert(data);
                    }
                },
                error: function () {
                    alert('failed');
                }
            });
        }
        catch (e) {
            alert("An error occurred: "+e);
        }
    };
};