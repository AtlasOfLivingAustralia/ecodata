

ecodata.forms["Beach_Sand_Photo_Point_MonitoringViewModel"] = function (output, dataModel, context, config) {
    var self = this;
    var site = context.site || {};
    self.name = output.name;
    self.outputId = output.outputId;
    self.data = {};
    self.transients = {};
    self.transients.dummy = ko.observable();


    // load dynamic models - usually objects in a list

    ecodata.forms.OutputModel.apply(self, [output, dataModel, context, config]);

    // add declarations for dynamic data

    self.data.surveyDate = ko.observable().extend({simpleDate: false});

    self.data.surveyStartTime = ko.observable();

    $('#surveyStartTimeTimeField').timeEntry({ampmPrefix: ' ', spinnerImage: '/assets/jquery.timeentry.package-2.0.1/spinnerOrange-fd47c3e0840f2d3e0fe43a0bf29627e3.png', spinnerBigImage: '/assets/jquery.timeentry.package-2.0.1/spinnerOrangeBig-2664f0b5582e668255fbc96e2a3d600a.png', spinnerSize: [20, 20, 8], spinnerBigSize: [40, 40, 16]});
    self.data.recordedBy = ko.observable();

    self.data.windDirectionCategorical = ko.observable().extend({metadata:{metadata:self.dataModel['windDirectionCategorical'], context:context, config:config}});

    self.data.windSpeedInBeaufortScale = ko.observable().extend({metadata:{metadata:self.dataModel['windSpeedInBeaufortScale'], context:context, config:config}});

    self.data.airTemperatureInDegreesCelsius = ko.observable().extend({numericString:2});

    self.data.otRainInPercent = ko.observable().extend({numericString:2});

    self.data.previousRainfall24HoursInMillimetres = ko.observable().extend({numericString:2});

    self.data.relativeHumidityIndexInPercentCategorical = ko.observable().extend({metadata:{metadata:self.dataModel['relativeHumidityIndexInPercentCategorical'], context:context, config:config}});

    self.data.recentStormEventLastSevenDays = ko.observable().extend({metadata:{metadata:self.dataModel['recentStormEventLastSevenDays'], context:context, config:config}});

    self.data.tideHeightCategorical = ko.observable().extend({metadata:{metadata:self.dataModel['tideHeightCategorical'], context:context, config:config}});

    self.data.tideDirectionCategorical = ko.observable().extend({metadata:{metadata:self.dataModel['tideDirectionCategorical'], context:context, config:config}});

    self.data.notableWeatherObservations = ko.observable();

    self.data.unusualWeatherConditions = ko.observableArray().extend({metadata:{metadata:self.dataModel['unusualWeatherConditions'], context:context, config:config}});

    self.loadunusualWeatherConditions = function (data) {
        if (data !== undefined) {
            self.data.unusualWeatherConditions(data);
        }};

    self.data.unusualWeatherConditionsOther = ko.observable();
    self.data.photoPointImage = ko.observableArray([]);

    self.loadphotoPointImage = function (data) {
        if (data !== undefined) {
            $.each(data, function (i, obj) {
                self.data.photoPointImage.push(new ImageViewModel(obj, false, context));
            });
        }};

    self.data.maxWaveHeightInMetres = ko.observable().extend({numericString:2});

    self.data.minWavePeriodInSeconds = ko.observable().extend({numericString:2});

    self.data.waveDirectionCategorical = ko.observable().extend({metadata:{metadata:self.dataModel['waveDirectionCategorical'], context:context, config:config}});

    self.data.unusualBeachConditions = ko.observableArray().extend({metadata:{metadata:self.dataModel['unusualBeachConditions'], context:context, config:config}});

    self.loadunusualBeachConditions = function (data) {
        if (data !== undefined) {
            self.data.unusualBeachConditions(data);
        }};
    var Output_beachSandPhotoPointMonitoring_groyneMeasurementsTableRow = function (data, dataModel, context, config) {
        var self = this;
        ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
        context = _.extend(context, {parent:self});
        self.sideOfGroyne = ko.observable();

        self.measurementPost1InMetres = ko.observable().extend({numericString:2});

        self.measurementPost3InMetres = ko.observable().extend({numericString:2});
        self.measurementPhoto = ko.observableArray([]);

        self.loadmeasurementPhoto = function (data) {
            if (data !== undefined) {
                $.each(data, function (i, obj) {
                    self.measurementPhoto.push(new ImageViewModel(obj, false, context));
                });
            }};
        self.loadData = function(data) {
            self['sideOfGroyne'](ecodata.forms.orDefault(data['sideOfGroyne'], undefined));
            self['measurementPost1InMetres'](ecodata.forms.orDefault(data['measurementPost1InMetres'], 0));
            self['measurementPost3InMetres'](ecodata.forms.orDefault(data['measurementPost3InMetres'], 0));
            self.loadmeasurementPhoto(ecodata.forms.orDefault(data['measurementPhoto'], []));
        };
        self.loadData(data || {});
    };
    var context = _.extend({}, context, {parent:self, listName:'groyneMeasurementsTable'});
    self.data.groyneMeasurementsTable = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_beachSandPhotoPointMonitoring_groyneMeasurementsTableRow, context:context, userAddedRows:false, config:config}});

    self.data.groyneMeasurementsTable.loadDefaults = function() {
        self.data.groyneMeasurementsTable.addRow({"measurementSFPost3InMetres":"","measurementSFPhoto":"","measurementSFPost1InMetres":"","sideOfGroyne":"SOUTH GROYNE : Front"});
        self.data.groyneMeasurementsTable.addRow({"measurementSBPost1InMetres":"","measurementSBPost3InMetres":"","measurementSBPhoto":"","sideOfGroyne":"SOUTH GROYNE : Back"});
        self.data.groyneMeasurementsTable.addRow({"measurementNFPhoto":"","measurementNFPost1InMetres":"","measurementNFPost3InMetres":"","sideOfGroyne":"NORTH GROYNE : Front"});
        self.data.groyneMeasurementsTable.addRow({"measurementNBPost3InMetres":"","measurementNBPhoto":"","measurementNBPost1InMetres":"","sideOfGroyne":"NORTH GROYNE  : Back"});
    };
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
                return ecodata.forms.expressionEvaluator.evaluate('(baselineHeightInMetres+opticalReading)-opticalReading', self, 2)
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
        self.loadData(data || {});
    };
    var context = _.extend({}, context, {parent:self, listName:'beachProfileRepeatSection'});
    self.data.beachProfileRepeatSection = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_beachSandPhotoPointMonitoring_beachProfileRepeatSectionRow, context:context, userAddedRows:true, config:config}});

    self.data.beachProfileRepeatSection.loadDefaults = function() {
        self.data.beachProfileRepeatSection.addRow();
    };


    self.data.locationLocality = ko.observable();


    self.data.locationAccuracy = ko.observable();


    self.data.locationNotes = ko.observable();


    self.data.locationSource = ko.observable();


    enmapify({
        viewModel: self
        , container: self.data
        , name: "location"
        , edit: true
        , readonly: false
        , zoomToProjectArea: true
        , markerOrShapeNotBoth: true
        , proxyFeatureUrl: '/acsa/proxy/feature'
        , spatialGeoserverUrl: 'http://spatial.ala.org.au/geoserver'
        , updateSiteUrl: '/acsa/site/ajaxUpdate'
        , listSitesUrl: '/acsa/site/ajaxList'
        , getSiteUrl: '/acsa/site/index'
        , uniqueNameUrl: '/acsa/site/checkSiteName'
        , activityLevelData: context
        , hideSiteSelection: false
        , hideMyLocation: true
        , context: config
    });
    self.loadData = function(data) {
        if (data['surveyDate'] || "'2019-01-29T17:47:07+1100'") {
            self.data['surveyDate'](ecodata.forms.orDefault(data['surveyDate'], '2019-01-29T17:47:07+1100'));
        }
        self.data['surveyStartTime'](ecodata.forms.orDefault(data['surveyStartTime'], undefined));
        self.data['recordedBy'](ecodata.forms.orDefault(data['recordedBy'], 'Sathish Sathyamoorthy'));
        self.data['windDirectionCategorical'](ecodata.forms.orDefault(data['windDirectionCategorical'], undefined));
        self.data['windSpeedInBeaufortScale'](ecodata.forms.orDefault(data['windSpeedInBeaufortScale'], undefined));
        self.data['airTemperatureInDegreesCelsius'](ecodata.forms.orDefault(data['airTemperatureInDegreesCelsius'], 0));
        self.data['otRainInPercent'](ecodata.forms.orDefault(data['otRainInPercent'], 0));
        self.data['previousRainfall24HoursInMillimetres'](ecodata.forms.orDefault(data['previousRainfall24HoursInMillimetres'], 0));
        self.data['relativeHumidityIndexInPercentCategorical'](ecodata.forms.orDefault(data['relativeHumidityIndexInPercentCategorical'], undefined));
        self.data['recentStormEventLastSevenDays'](ecodata.forms.orDefault(data['recentStormEventLastSevenDays'], undefined));
        self.data['tideHeightCategorical'](ecodata.forms.orDefault(data['tideHeightCategorical'], undefined));
        self.data['tideDirectionCategorical'](ecodata.forms.orDefault(data['tideDirectionCategorical'], undefined));
        self.data['notableWeatherObservations'](ecodata.forms.orDefault(data['notableWeatherObservations'], undefined));
        self.data['unusualWeatherConditions'](ecodata.forms.orDefault(data['unusualWeatherConditions'], []));
        self.data['unusualWeatherConditionsOther'](ecodata.forms.orDefault(data['unusualWeatherConditionsOther'], undefined));
        self.loadphotoPointImage(ecodata.forms.orDefault(data['photoPointImage'], []));
        self.data['maxWaveHeightInMetres'](ecodata.forms.orDefault(data['maxWaveHeightInMetres'], 0));
        self.data['minWavePeriodInSeconds'](ecodata.forms.orDefault(data['minWavePeriodInSeconds'], 0));
        self.data['waveDirectionCategorical'](ecodata.forms.orDefault(data['waveDirectionCategorical'], undefined));
        self.data['unusualBeachConditions'](ecodata.forms.orDefault(data['unusualBeachConditions'], []));
        self.loadgroyneMeasurementsTable(data.groyneMeasurementsTable);
        self.loadbeachProfileRepeatSection(data.beachProfileRepeatSection);

        if (data.location && typeof data.location !== 'undefined') {
            self.data.location(data.location);
        } else {
            self.loadActivitySite();
        }

        if (data.locationLatitude && typeof data.locationLatitude !== 'undefined') {
            self.data.locationLatitude(data.locationLatitude);
        }
        if (data.locationLongitude && typeof data.locationLongitude !== 'undefined') {
            self.data.locationLongitude(data.locationLongitude);
        }
        if (data.locationAccuracy){
            if( typeof data.locationAccuracy !== 'undefined') {
                self.data.locationAccuracy(data.locationAccuracy);
            }
        } else if((typeof 10 !== 'undefined') && self.data.locationAccuracy) {
            self.data.locationAccuracy(10);
        }

        if (data.locationLocality && typeof data.locationLocality !== 'undefined') {
            self.data.locationLocality(data.locationLocality);
        }
        if (data.locationSource && typeof data.locationSource !== 'undefined') {
            self.data.locationSource(data.locationSource);
        }
        if (data.locationNotes && typeof data.locationNotes !== 'undefined') {
            self.data.locationNotes(data.locationNotes);
        }
    };


    // this will be called when generating a savable model to remove transient properties
    self.removeBeforeSave = function (jsData) {
        delete jsData.selectedgroyneMeasurementsTableRow;
        delete jsData.groyneMeasurementsTableTableDataUploadOptions
        delete jsData.groyneMeasurementsTableTableDataUploadVisible
        delete jsData.selectedbeachProfileRepeatSectionRow;
        delete jsData.beachProfileRepeatSectionTableDataUploadOptions
        delete jsData.beachProfileRepeatSectionTableDataUploadVisible
        delete jsData.data.locationLatLonDisabled;
        delete jsData.data.locationSitesArray;
        delete jsData.data.locationLoading;
        delete jsData.data.locationMap;

        return self.removeTransients(jsData);
    };

    self.reloadGeodata = function() {
        console.log('Reloading geo fields')
        // load dynamic data

        var oldlocation = self.data.location()
        if(oldlocation) {
            self.data.location(null)
            self.data.location(oldlocation);
        }

        var oldlocationLatitude = self.data.locationLatitude()
        var oldlocationLongitude = self.data.locationLongitude()

        if(oldlocationLatitude) {
            self.data.locationLatitude(null)
            self.data.locationLatitude(oldlocationLatitude)
        }

        if(oldlocationLongitude) {
            self.data.locationLongitude(null)
            self.data.locationLongitude(oldlocationLongitude)
        }


    }
};
