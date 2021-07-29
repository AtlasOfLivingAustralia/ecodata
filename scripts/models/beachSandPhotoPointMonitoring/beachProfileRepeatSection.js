// "jsClass": "https://dl.dropbox.com/s/pvg4m964064crjp/beachProfileRepeatSection.js?dl=0"
var Output_beachSandPhotoPointMonitoring_beachProfileRepeatSectionRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    self.transectLocation = ko.observable().extend({metadata:{metadata:self.dataModel['transectLocation'], context:context, config:config}});

    self.transectBearingInDegrees = ko.observable().extend({numericString:2});

    self.beachWidthInMetres = ko.observable().extend({numericString:2});

    self.baselineHeightInMetres = ko.observable().extend({numericString:2});

    self.highWaterMarkInMetres = ko.observable().extend({numericString:2});

    self.sandHeightInMetres = ko.observable().extend({numericString:2});

    var Output_beachSandPhotoPointMonitoring_beachProfileMeasurementsTableRow = function (data, dataModel, context, config) {
        var self = this;
        ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
        context = _.extend(context, {parent:self});
        self.measurementFromSeaWallInMetres = ko.observable().extend({numericString:2});

        self.substrate = ko.observable().extend({metadata:{metadata:self.dataModel['substrate'], context:context, config:config}});

        self.elevation = ko.observable().extend({metadata:{metadata:self.dataModel['elevation'], context:context, config:config}});

        self.opticalReading = ko.observable().extend({numericString:2});
        self.calculatedProfileValue = ko.computed(function () {
            //Hook in to calculate baselineHeightInMetresPlusOpticalReading = (baselineHeightInMetres+firstRowopticalReading)
            return ecodata.forms.expressionEvaluator.evaluate('baselineHeightInMetresPlusOpticalReading-opticalReading', self, 2)
        });
        self.loadData = function(data) {
            self['measurementFromSeaWallInMetres'](ecodata.forms.orDefault(data['measurementFromSeaWallInMetres'], 0));
            self['substrate'](ecodata.forms.orDefault(data['substrate'], undefined));
            self['elevation'](ecodata.forms.orDefault(data['elevation'], undefined));
            self['opticalReading'](ecodata.forms.orDefault(data['opticalReading'], 0));
        };
        self.loadData(data || {});
    };
    var context = _.extend({}, context, {parent:self, listName:'beachProfileMeasurementsTable'});
    self.beachProfileMeasurementsTable = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_beachSandPhotoPointMonitoring_beachProfileMeasurementsTableRow, context:context, userAddedRows:true, config:config}});

    self.beachProfileMeasurementsTable.loadDefaults = function() {
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"0","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
        self.beachProfileMeasurementsTable.addRow({"elevation":"","calculatedProfileValue":"","substrate":"","measurementFromSeaWallInMetres":"","opticalReading":""});
    };

    self.loadData = function(data) {
        self['transectLocation'](ecodata.forms.orDefault(data['transectLocation'], undefined));
        self['transectBearingInDegrees'](ecodata.forms.orDefault(data['transectBearingInDegrees'], 0));
        self['beachWidthInMetres'](ecodata.forms.orDefault(data['beachWidthInMetres'], 0));
        self['baselineHeightInMetres'](ecodata.forms.orDefault(data['baselineHeightInMetres'], 0));
        self['highWaterMarkInMetres'](ecodata.forms.orDefault(data['highWaterMarkInMetres'], 0));
        self['sandHeightInMetres'](ecodata.forms.orDefault(data['sandHeightInMetres'], 0));
        self.loadbeachProfileMeasurementsTable(data.beachProfileMeasurementsTable);
    };

    // Hook in to calculate baseline+opticalreading
    self.baselineHeightInMetresPlusOpticalReading = ko.computed(function () {
        var firstRow = 0.0;
        for (var i=0; i < self.beachProfileMeasurementsTable().length; i++) {
            if(i == 0) {
                var row = self.beachProfileMeasurementsTable()[i];
                firstRow = parseFloat(row.opticalReading())
            }
            break;
        }
        var result = firstRow + parseFloat(self.baselineHeightInMetres());
        return result;
    });

    self.loadData(data || {});
};

var context = _.extend({}, context, {parent:self, listName:'beachProfileRepeatSection'});
self.data.beachProfileRepeatSection = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_beachSandPhotoPointMonitoring_beachProfileRepeatSectionRow, context:context, userAddedRows:true, config:config}});

self.data.beachProfileRepeatSection.loadDefaults = function() {
    self.data.beachProfileRepeatSection.addRow();
};