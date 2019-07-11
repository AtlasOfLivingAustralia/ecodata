var EditActivityFormSectionViewModel = function (availableForms, selectedFormName, service) {
    var self = this;
    this.selectedActivityForm = ko.observable();
    this.selectedFormName = ko.observable();
    this.selectedFormVersion = ko.observable();
    this.activityFormVersions = ko.observableArray();
    this.activityForms = ko.observableArray(availableForms);
    this.warning = ko.observable();

    self.selectedFormVersion.subscribe(function(version) {
        self.loadSelectedActivity();
    });

    self.loadSelectedActivity = function() {
        var name = self.selectedFormName();
        var version = self.selectedFormVersion();

        if (name && version) {
            service.loadActivityForm(name, version).done(function(activityForm) {
                self.selectedActivityForm(activityForm);

                service.findUsersOfForm(activityForm).done(function(result) {
                    if (result && result.count > 0) {
                        self.warning("There are "+result.count+" activities with data for this version of the form");
                    }
                    else {
                        self.warning("");
                    }
                });
            });
        }
        else {
            self.warning("");
        }
    };

    self.selectedFormName.subscribe(function(selected) {

        if (!selected) {
            self.activityFormVersions([]);
            self.selectedActivityForm(null);
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

    if (selectedFormName) {
        var toSelect = _.find(self.activityForms(), function(form) {
            return form.name == selectedFormName;
        });
        if (toSelect) {
            self.selectedFormName(toSelect);
        }
    }
};