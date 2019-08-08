
/**
 * View model for editing activity form templates
 * @param availableForms a list of form names and versions for selection.
 * @param service used to communitcate with ecodata
 */
var EditActivityTemplatesViewModel = function(availableForms, selectedForm, service, config) {
    var self = this;

    var editor = null;
    function nodeName(path, type, size) {
        var result = editor.getNodesByRange(path);
        var value = result[0].value;
        var resultPath = result[0].path;
        if (resultPath[0] == "dataModel" && resultPath.length > 1) {
            return value.name+" ("+value.dataType+")";
        }
        else if (resultPath[0] == "viewModel" && resultPath.length > 1) {
            var name = "("+value.type+")";
            if (value.source) {
                name += " source = "+value.source;
            }
            return name;
        }
    }
    var editorPane = document.getElementById("jsoneditor");

    if (editorPane) {
        var options = {
            mode: 'code',
            modes: ['code', 'tree'],
            onNodeName: nodeName
        };
        editor = new JSONEditor(editorPane, options);
    }

    this.modelName = ko.observable('<No form section selected>');

    self.selectionModel = new EditActivityFormSectionViewModel(availableForms, selectedForm, service, config);
    self.selectedActivityForm = self.selectionModel.selectedActivityForm;

    self.availableFormSections = ko.observableArray();

    self.selectionModel.selectedActivityForm.subscribe(function(activityForm) {
        if (activityForm) {
            self.availableFormSections(activityForm.sections || []);
            if (activityForm.sections.length == 1) {
                self.selectedFormSection(activityForm.sections[0]);
            }
        }
        else {
            self.availableFormSections([]);
        }

    });

    this.selectedFormSection = ko.observable();
    this.message = ko.observable('');
    this.hasMessage = ko.computed(function () {
        return self.message() !== ''
    });
    this.warning = ko.observable('');

    this.save = function () {

        var activityForm = self.selectedActivityForm();
        var selectedSection = self.selectedFormSection();
        var template = editor.get();

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

        editor.set(data);
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
                document.location.href = config.reloadUrl+"?form="+encodeURIComponent(activityForm.name);
            });
        }
    };

    self.publishForm = function() {
        var activityForm = self.selectedActivityForm();
        if (activityForm && activityForm.publicationStatus != 'published') {
            service.publish(activityForm).done(function(form) {
                document.location.href = config.reloadUrl+"?form="+encodeURIComponent(activityForm.name);
            });
        }
    };

    self.unpublishForm = function() {
        var activityForm = self.selectedActivityForm();
        if (activityForm && activityForm.publicationStatus == 'published') {
            service.unpublish(activityForm).done(function(form) {
                document.location.href = config.reloadUrl+"?form="+encodeURIComponent(activityForm.name);
            });
        }
    };

    self.importActivity = function(vm, e) {
        service.loadFromFile(e).done(function(data) {
            var selected = self.selectedActivityForm();
            if (selected.publicationStatus != "unpublished") {
                alert("You can't import into a published form!");
                return;
            }
            if (selected.name != data.name) {
                alert("You appear to be importing a different activity form.  Use Edit activity form definitions to create a new form");
                return;
            }
            data.formVersion = selected.formVersion;
            service.update(data);
        });
    };

    self.exportActivity = function() {
        service.export(self.selectedActivityForm());
    };

    self.clearTemplate = function() {
        editor.set({});
        self.modelName("<No form section selected>");
    };
};






