

var ProgramModel = function (prg, model) {
    var self = this;
    self.name = ko.observable(prg.name);

    self.subprograms = ko.observableArray($.map(prg.subprograms, function (subprogram) {
        return new SubprogramModel(subprogram, self, model);
    }));

    self.isMeritProgramme = ko.observable(prg.isMeritProgramme);
    self.reportingPeriod = ko.observable(prg.reportingPeriod);
    self.reportingPeriodAlignedToCalendar = ko.observable(prg.reportingPeriodAlignedToCalendar);
    self.projectDatesContracted = ko.observable(prg.projectDatesContracted);
    self.optionalProjectContent = ko.observableArray(prg.optionalProjectContent || []);
    self.weekDaysToCompleteReport = ko.observable(prg.weekDaysToCompleteReport);
    self.activities = ko.observableArray(prg.activities?prg.activities:[]);
    self.speciesFieldsSettings = ko.observable().extend({jsonText:prg.speciesFieldsSettings});
    self.select = function () {
        model.transients.selectedProgram(this);
        model.transients.selectedSubprogram(undefined);
    };
    self.isSelected = ko.computed(function () {
        return self === model.transients.selectedProgram();
    });
    self.transients = {};
    self.transients.showActivities = ko.observable(false);
    self.transients.showSpeciesSettings = ko.observable(false);

    self.toggleSpeciesSettings = function() {
        self.transients.showSpeciesSettings(!self.transients.showSpeciesSettings());
    };
    self.toggleActivities = function() {
        self.transients.showActivities(!self.transients.showActivities());
    };

    self.toJSON = function() {
        var js = ko.toJS(this);
        js.weekDaysToCompleteReport = Number(js.weekDaysToCompleteReport);
        js.speciesFieldsSettings = self.speciesFieldsSettings.toJSON();
        delete js.isSelected;
        delete js.transients;
        return js;
    }
};

var SubprogramModel = function (subProgram, programModel, model) {
    var self = this;
    self.name = ko.observable(subProgram.name);
    self.startDate = ko.observable(subProgram.startDate).extend({simpleDate:false});
    self.endDate = ko.observable(subProgram.endDate).extend({simpleDate:false});
    self.optionalProjectContent = ko.observableArray(subProgram.optionalProjectContent || []);
    self.weekDaysToCompleteReport = ko.observable(subProgram.weekDaysToCompleteReport);

    self.themes = ko.observableArray($.map(subProgram.themes, function (obj) {
        return new ThemeModel(obj, model);
    }));
    self.overridesProgramData = ko.observable(subProgram.overridesProgramData);
    self.reportingPeriod = ko.observable(subProgram.reportingPeriod);
    self.reportingPeriodAlignedToCalendar = ko.observable(subProgram.reportingPeriodAlignedToCalendar);
    self.projectDatesContracted = ko.observable(subProgram.projectDatesContracted);

    self.activities = ko.observableArray(subProgram.activities || []);
    self.speciesFieldsSettings = ko.observable().extend({jsonText:subProgram.speciesFieldsSettings});

    self.select = function () {
        model.transients.selectedSubprogram(this);
    };
    self.isSelected = ko.computed(function () {
        return self === model.transients.selectedSubprogram();
    });

    self.transients = {};
    self.transients.showActivities = ko.observable(false);
    self.transients.showSpeciesSettings = ko.observable(false);

    self.transients.showSpeciesSettings = ko.observable(false);

    self.toggleSpeciesSettings = function() {
        self.transients.showSpeciesSettings(!self.transients.showSpeciesSettings());
    };


    self.toggleActivities = function() {
        self.transients.showActivities(!self.transients.showActivities());
    };
    self.overridesProgramData.subscribe(function(newValue) {
        if (!newValue) {
            self.optionalProjectContent([]);
            self.weekDaysToCompleteReport(undefined);
            self.reportingPeriod(undefined);
            self.reportingPeriodAlignedToCalendar(undefined);
            self.projectDatesContracted(undefined);
            self.activities([]);
        }
        else {
            self.optionalProjectContent(programModel.optionalProjectContent() || []);
            self.weekDaysToCompleteReport(programModel.weekDaysToCompleteReport());
            self.reportingPeriod(programModel.reportingPeriod());
            self.reportingPeriodAlignedToCalendar(programModel.reportingPeriodAlignedToCalendar());
            self.projectDatesContracted(programModel.projectDatesContracted());
            self.activities(programModel.activities() || []);
        }
    });
    self.toJSON = function() {
        var js = ko.toJS(this);
        if (js.weekDaysToCompleteReport) {
            js.weekDaysToCompleteReport = Number(js.weekDaysToCompleteReport);
        }
        js.speciesFieldsSettings = self.speciesFieldsSettings.toJSON();
        delete js.isSelected;
        delete js.transients;
        return js;
    }
};

var ThemeModel = function (theme, model) {
    var self = this;
    self.name = ko.observable(theme.name);

    self.select = function () {
        model.transients.selectedTheme(this);
    };

    self.isSelected = ko.computed(function () {
        return self === model.transients.selectedTheme();
    });
    self.toJSON = function() {
        var js = ko.toJS(this);
        delete js.isSelected;
        return js;
    }
};

var ProgramModelViewModel = function (model, activityTypes, options) {
    var self = this;
    var defaults = {
        updateProgramsModelUrl : fcConfig.updateProgramsModelUrl
    };
    var config = $.extend(defaults, options);

    self.transients = {};
    self.transients.selectedProgram = ko.observable();
    self.transients.selectedSubprogram = ko.observable();
    self.transients.selectedTheme = ko.observable();
    self.transients.activityTypes = activityTypes;
    self.transients.optionalProjectContent = ['MERI Plan', 'Risks and Threats'];

    self.programs = ko.observableArray($.map(model.programs, function (obj, i) {
        return new ProgramModel(obj, self);
    }));

    self.transients.displayedSubprograms = ko.computed(function () {
        return (self.transients.selectedProgram() !== undefined) ?
            self.transients.selectedProgram().subprograms() : [];
    });
    self.transients.displayedThemes = ko.computed(function () {
        if (self.transients.selectedProgram() === undefined) { return [] }
        if (self.transients.selectedSubprogram() === undefined) { return [] }
        return self.transients.selectedSubprogram().themes();
    });
    self.addProgram = function (item, event) {
        var act = new ProgramModel({name: "", subprograms: [], optionalProjectContent:self.transients.optionalProjectContent}, self);
        self.programs.push(act);
        act.name.editing(true);
    };
    self.addSubprogram = function (item, event) {
        var newSub = new SubprogramModel({name:"", themes:[]}, self.transients.selectedProgram(), self);
        self.transients.selectedProgram().subprograms.push(newSub);
        newSub.name.editing(true);
    };
    self.addTheme = function (item, event) {
        var newTheme = new ThemeModel({name:""}, self);
        self.transients.selectedSubprogram().themes.push(newTheme);
        newTheme.name.editing(true);
    };
    self.removeProgram = function () {
        self.programs.remove(this);
    };
    self.removeSubprogram = function () {
        self.transients.selectedProgram().subprograms.remove(this);
    };
    self.removeTheme = function () {
        self.transients.selectedSubprogram().themes.remove(this);
    };
    self.revert = function () {
        document.location.reload();
    };
    self.save = function () {
        var model = ko.toJS(self);
        delete model.transients;
        $.ajax(config.updateProgramsModelUrl, {
            type: 'POST',
            data: vkbeautify.json(model,2),
            contentType: 'application/json',
            success: function (data) {
                if (data !== 'error') {
                    document.location.reload();
                } else {
                    alert(data);
                }
            },
            error: function () {
                alert('failed');
            },
            dataType: 'text'
        });
    };
};