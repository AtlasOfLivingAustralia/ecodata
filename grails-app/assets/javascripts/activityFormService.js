
var ActivityFormService = function(config) {
    var self = this;

    var saveActivityForm = function(activityForm, url) {
        return $.ajax({
            url:url,
            type: 'POST',
            dataType:'json',
            data: JSON.stringify(activityForm),
            contentType: 'application/json'
        }).done(function (data) {
            if (data.error) {
                alert(data.message);
            } else {
                document.location.href = config.reloadUrl+"?form="+encodeURIComponent(activityForm.name);
            }

        }).fail(function() {
            alert("An error occurred saving your template data.")
        });
    };

    self.update = function(activityForm) {
        return saveActivityForm(activityForm, config.activityFormUpdateUrl);
    };
    self.create = function(activityForm) {
        return saveActivityForm(activityForm, config.activityFormCreateUrl);
    };

    self.loadActivityForm = function(name, version) {
        return $.getJSON(config.getActivityFormUrl, {name:name.name, formVersion:version}).fail(function() {
            alert("An error occurred loading the selected form");
        });
    };

    self.newDraft = function(activityForm) {
        var name = activityForm.name;
        var url = config.newDraftFormUrl+"?name="+encodeURIComponent(name);
        return $.ajax({
            url:url,
            type: 'POST',
            dataType:'json',
            contentType: 'application/json'
        }).fail(function() {
            alert("Draft creation failed");
        });
    };

    self.publish = function(activityForm) {
        var name = activityForm.name;
        var formVersion = activityForm.formVersion;
        var url = config.publishActivityFormUrl+"?name="+encodeURIComponent(name)+"&formVersion="+formVersion;
        return $.ajax({
            url:url,
            type: 'POST',
            dataType:'json',
            contentType: 'application/json'
        }).fail(function() {
            alert("Draft creation failed");
        });
    };

    self.unpublish = function(activityForm) {
        var name = activityForm.name;
        var formVersion = activityForm.formVersion;
        var url = config.unpublishActivityFormUrl+"?name="+encodeURIComponent(name)+"&formVersion="+formVersion;
        return $.ajax({
            url:url,
            type: 'POST',
            dataType:'json',
            contentType: 'application/json'
        }).fail(function() {
            alert("Draft creation failed");
        });
    };

    self.findUsersOfForm = function(activityForm) {
        return $.getJSON(config.findUsersOfFormUrl, {name:activityForm.name, formVersion:activityForm.formVersion}).fail(function() {
            alert("An error occurred loading the selected form");
        });
    };


    self.export = function(activityForm) {

        var jsonData = vkbeautify.json(activityForm, 2);
        var fileName = activityForm.name + " v"+activityForm.formVersion+".json"
        function download(content, fileName, contentType) {
            var a = document.createElement("a");
            var file = new Blob([content], {type: contentType});
            a.href = URL.createObjectURL(file);
            a.download = fileName;
            a.click();
        }
        download(jsonData, fileName, 'application/json');
    };

    self.loadFromFile = function(e) {
        if (typeof window.FileReader !== 'function') {
            alert("The file API isn't supported on this browser yet.");
            return;
        };
        var files = e.target.files;
        if (!files[0]) {
            return;
        }
        var file = files[0];
        var fr = new FileReader();
        var deferred = $.Deferred();
        fr.onload = function(e) {
            var data = JSON.parse(e.target.result);
            deferred.resolve(data);
        };
        fr.readAsText(file);
        return deferred;
    }
};