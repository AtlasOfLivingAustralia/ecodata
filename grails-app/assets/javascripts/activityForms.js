var $textarea = $('#outputModelEdit');

/**
 * View model for editing activity form templates
 * @param availableForms a list of form names and versions for selection.
 * @param config configuration items.
 * @constructor
 */
var EditActivityFormSectionViewModel = function (availableForms, config) {
    var self = this;
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

        $.ajax({
            url:config.activitFormUpdateUrl,
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
            $.getJSON(config.getActivityFormUrl, {name:name.name, formVersion:version}).done(function(activityForm) {
                self.selectedActivityForm(activityForm);
                if (activityForm.sections.length == 1) {
                    self.selectedFormSection(activityForm.sections[0]);
                }

            }).fail(function() {
                alert("An error occurred loading the selected form");
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

    self.clearTemplate = function() {
        $textarea.val('');
        self.modelName("<No form section selected>");
    }
};




