
var ActivityFormViewModel = function (act, model) {
    var self = this;
    self.name = ko.observable(act.name);
    self.description = ko.observable(act.description);
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
    self.description = ko.observable(formSection.description);
    self.optionalQuestionText = ko.observable(formSection.optionalQuestionText);
    self.optional = ko.observable(formSection.optional);
    self.collapsedByDefault = ko.observable(formSection.collapsedByDefault);
    var templateName = formSection.templateName || (parent.name() + '_' + self.name());
    self.templateName = ko.observable(templateName);

    var defaultTemplate = {
        modelName:self.name,
        dataModel:[],
        viewModel:[],
        title:self.name
    };
    self.template = formSection.template || defaultTemplate;

    self.optional.subscribe(function (val) {
        if (!val) {
            self.collapsedByDefault(false);
        }
    });
};


var ActivityModelViewModel = function (model, selectedForm, service, config) {
    var self = this;

    self.selectionModel = new EditActivityFormSectionViewModel(model, selectedForm, service, config);
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


    self.exportActivity = function() {
        service.export(self.selectedActivity().toJSON());
    };

    self.importActivity = function(vm, e) {
        service.loadFromFile(e).done(function(data) {
            data.name = "Copy of "+data.name;
            data.formVersion = 1;
            _.each(data.sections, function(formSection) {
                formSection.name = "Copy of "+formSection.name;
                formSection.templateName = "copyOf"+formSection.templateName;
            });

            var activityForm = new ActivityFormViewModel(data, self);

            self.selectedActivity(activityForm);
            activityForm.expanded(true);
            activityForm.editing(true);
            self.editingNew = true;
        });
    };

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
