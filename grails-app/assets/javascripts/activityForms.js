var $textarea = $('#outputModelEdit');

/**
 * View model for editing activity form templates
 * @param availableForms a list of form names and versions for selection.
 * @param service used to communitcate with ecodata
 */
var EditActivityFormSectionViewModel = function (availableForms, service) {
    var self = this;

    this.modelName = ko.observable('<No form section selected>');

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

        service.update(activityForm);
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
                $textarea.html(vkbeautify.json(data, 2));
                document.location.reload();
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
};

var ActivityFormViewModel = function (act, model) {
    var self = this;
    self.name = ko.observable(act.name);
    self.type = ko.observable(act.type);
    self.sections = ko.observableArray($.map(act.sections || [], function (formSection) {
        return new FormSection(formSection, self);
    }));
    self.formVersion = 1;
    self.expanded = ko.observable(false);
    self.category = ko.observable(act.category);
    self.enabled = ko.observable(!act.status || act.status == 'active');
    self.status = ko.observable(act.status);
    self.gmsId = ko.observable(act.gmsId);
    self.supportsSites = ko.observable(act.supportsSites);
    self.supportsPhotoPoints = ko.observable(act.supportsPhotoPoints);
    self.minOptionalSectionsCompleted = ko.observable(act.minOptionalSectionsCompleted || 1);

    self.enabled.subscribe(function (enabled) {
        if (enabled) {
            self.status('active');
        }
        else {
            self.status('deleted');
        }
    });
    self.toggle = function (data, event) {
        if (!self.expanded()) {
            $.each(model.activities(), function (i, obj) {
                obj.expanded(false); // close all
                obj.done(); // exit editing mode
            });
            self.expanded(true);
            model.selectedActivity(self);
        } else {
            self.expanded(false);
            self.done(); // in case we were editing
            model.selectedActivity(undefined);
        }
    };
    self.type.subscribe(function (newType) {
        if (newType === 'Report') {
            self.supportsSites(false);
            self.supportsPhotoPoints(false);
        }
    });
    self.editing = ko.observable(false);
    self.edit = function () {
        self.editing(true)
    };
    self.done = function () {
        self.editing(false)
    };
    self.displayMode = function () {
        return self.editing() ? 'editActivityTmpl' : 'viewActivityTmpl';
    };
    self.removeFormSection = function (toRemove) {
        self.sections.remove(function (formSection) {
            return toRemove == formSection;
        });
    };
    self.addSection = function () {
        var name = self.name();
        var i = 1;
        while (_.find(self.sections(), function(formSection) { return formSection.name() == name; })) {
            i++;
            name = self.name() + " ("+i+")";
        };

        var section = new FormSection({name: name}, self);
        self.sections.push(section);
    };


    self.toJSON = function () {
        var js = ko.toJS(this);
        delete js.expanded;
        delete js.editing;
        delete js.enabled;
        return js;
    };

};

var FormSection = function (formSection, parent) {
    var self = this;
    self.name = ko.observable(formSection.name);
    self.optionalQuestionText = ko.observable(formSection.optionalQuestionText);
    self.optional = ko.observable(formSection.optional);
    self.collapsedByDefault = ko.observable(formSection.collapsedByDefault);
    self.templateName = ko.computed(function() {
        return formSection.templateName || (parent.name() + ' - ' + self.name());
    });
    self.template = {
        modelName:self.name,
        dataModel:[],
        viewModel:[],
        title:self.name
    };

    self.optional.subscribe(function (val) {
        if (!val) {
            self.collapsedByDefault(false);
        }
    });
};


var ActivityModelViewModel = function (model, service) {
    var self = this;

    self.selectionModel = new EditActivityFormSectionViewModel(model, service);
    self.editingNew = false;
    self.selectionModel.selectedActivityForm.subscribe(function(form) {
        if (form) {
            self.editingNew = false;
            self.selectedActivity(new ActivityFormViewModel(form));
        }
        else {
            self.selectedActivity(null);
        }
    });

    self.selectedActivity = ko.observable();

    self.addActivity = function () {
        self.editingNew = true;
        var act = new ActivityFormViewModel({name: 'New activity', type: 'Activity'}, self);
        act.addSection();
        self.selectedActivity(act);
        act.expanded(true);
        act.editing(true);
    };

    self.revert = function () {
        document.location.reload();
    };
    self.save = function () {
        var activityForm = self.selectedActivity().toJSON();
        if (self.editingNew) {
            service.create(activityForm);
        }
        else {
            service.update(activityForm);
        }

    };

};





