var $textarea = $('#outputModelEdit');

/**
 * View model for editing activity form templates
 * @param availableForms a list of form names and versions for selection.
 * @param config configuration items.
 * @constructor
 */
var EditActivityFormSectionViewModel = function (availableForms, config) {
    var self = this;

    var service = new ActivityFormService(config);

    this.modelName = ko.observable('No output selected');

    this.transients = {};
    this.selectedActivityForm = ko.observable();
    this.selectedFormName = ko.observable();
    this.selectedFormVersion = ko.observable();
    this.activityFormVersions = ko.observableArray();
    this.activityForms = ko.observableArray(availableForms);
    this.selectedFormSection = ko.observable();
    this.message = ko.observable('');
    this.hasMessage = ko.computed(function () {
        return self.message() !== ''
    });

    this.save = function () {

        var activityForm = self.selectedActivityForm();

        var selectedSection = self.selectedFormSection();
        var template = JSON.parse($textarea.val());

        var formSection = _.find(activityForm.sections || [], function(section) {
            return section.name == selectedSection.name;
        });
        formSection.template = template;

        service.saveActivityForm(activityForm);
    };

    this.revert = function () {
        document.location.reload();
    };

    this.displayDataModel = function (data) {

        self.modelName(data.modelName);

        if (self.selectedActivityForm().publicationStatus == 'published') {
            self.message("You are editing a published form");
        }
        else {
            self.message("");
        }

        $textarea.val(vkbeautify.json(data, 2));
    };


    self.selectedFormName.subscribe(function(selected) {

        if (!selected) {
            self.activityFormVersions([]);
            self.clearTemplate();
            self.selectedActivityForm(null);
            self.selectedFormSection(null);
            self.message("");
        }
        else {
            var formVersions = selected && selected.formVersions || [];
            self.activityFormVersions(formVersions);
            if (formVersions.length > 0) {
                self.selectedFormVersion(formVersions[0]);
            }
            self.loadSelectedActivity();
        }

    });

    self.selectedFormVersion.subscribe(function(version) {
        self.loadSelectedActivity();
    });

    self.loadSelectedActivity = function() {
        var name = self.selectedFormName();
        var version = self.selectedFormVersion();

        if (name && version) {
            service.loadActivityForm(name, version).done(function(activityForm) {
                self.selectedActivityForm(activityForm);
                if (activityForm.sections.length == 1) {
                    self.selectedFormSection(activityForm.sections[0]);
                }
            });
        }
    };

    this.selectedFormSection.subscribe(function(formSection) {

        if (formSection) {
            self.displayDataModel(formSection.template);
        }
        else {
            self.clearTemplate();
        }
    });

    self.newDraftForm = function() {
        var activityForm = self.selectedActivityForm();
        if (activityForm && activityForm.publicationStatus == 'published') {
            service.newDraft(activityForm).done(function(form) {
                document.location.reload();
            });
        }
    };

    self.publishForm = function() {
        var activityForm = self.selectedActivityForm();
        if (activityForm && activityForm.publicationStatus != 'published') {
            service.publish(activityForm).done(function(form) {
                document.location.reload();
            });
        }
    };

    self.unpublishForm = function() {
        var activityForm = self.selectedActivityForm();
        if (activityForm && activityForm.publicationStatus == 'published') {
            service.unpublish(activityForm).done(function(form) {
                document.location.reload();
            });
        }
    };

    self.clearTemplate = function() {
        $textarea.val('');
        self.modelName("<No form section selected>");
    }
};

var ActivityFormService = function(config) {
    var self = this;

    self.saveActivityForm = function(activityForm) {
        return $.ajax({
            url:config.activityFormUpdateUrl,
            type: 'POST',
            dataType:'json',
            data: JSON.stringify(activityForm),
            contentType: 'application/json'
        }).done(function (data) {
            if (data.error) {
                alert(data.message);
            } else {
                $textarea.html(vkbeautify.json(data, 2));
                document.location.reload();
            }

        }).fail(function() {
            alert("An error occurred saving your template data.")
        });
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
};




