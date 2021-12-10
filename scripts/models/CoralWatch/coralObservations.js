// https://dl.dropbox.com/s/gj71mpffihldk7f/coralObservations.js?dl=0
var imageCarousel = '<img style="height:300px" src="https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&filename=plate.png&model=coralWatch" alt="Plate">'+
    '<img style="height:300px" src="https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&model=coralWatch&filename=boulder_layout.png" alt="Boulder">' +
    '<img style="height:300px" src="https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&model=coralWatch&filename=branching.png" alt="Branching">'+
    '<img style="height:300px" src="https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&model=coralWatch&filename=soft.png" alt="Boulder">';
$(".form-actions").before( imageCarousel ) ;

const coralWatchContainerValueIncrement = function coralWatchContainerValueIncrement(container, key) {
    if (!container || !key) {
        return;
    }
    if (!container.has(key)) {
        container.set(key, 0);
    }
    const newValue = container.get(key) + 1;
    container.set(key, newValue);
};

/**
 * Update the knockout array using the new map.
 * @param currentKoArray The current knockout array.
 * @param newMap {Map} The new map.
 */
const coralWatchContainerToChartDataItems = function coralWatchContainerToChartDataItems(currentKoArray, newMap) {
    console.warn('[CoralWatch] coralWatchContainerToChartDataItems start', currentKoArray(), newMap);
    if (!currentKoArray) {
        return;
    }
    if (!newMap) {
        newMap = new Map();
    }

    const prefix = 'CustomChartDataItem';
    const sep = '|||';

    const newItems = [];

    // add or update entries from new map
    newMap.forEach(function (value, key) {
        const newItem = prefix + sep + key.toString() + sep + value.toString();
        newItems.push(newItem);

        if (currentKoArray.indexOf(newItem) === -1) {
            const newPartialItem = prefix + sep + key.toString() + sep;
            const existingKeyIndex = currentKoArray().findIndex(function (currentArrayValue) {
                return currentArrayValue.startsWith(newPartialItem);
            });
            if (existingKeyIndex > -1) {
                // update existing entry with same key but different value
                currentKoArray.splice(existingKeyIndex, 1, newItem);
            } else {
                // add new entry
                currentKoArray.push(newItem);
            }
        }
    });

    // remove entries that are not present in new map
    // .remove(func) is a knockout extension to array
    currentKoArray.remove(function (element) {
        return newItems.indexOf(element) === -1;
    });

    console.warn('[CoralWatch] coralWatchContainerToChartDataItems finish', currentKoArray());
};

const coralWatchRecordLevelCountUpdate = function (items) {
    // count the number of each unique value
    const colourCodeAverages = new Map();
    const typeOfCorals = new Map();
    items.forEach(function (item) {
        const colourCodeAverage = item.colourCodeAverage();
        coralWatchContainerValueIncrement(colourCodeAverages, colourCodeAverage);

        const typeOfCoral = item.typeOfCoral();
        coralWatchContainerValueIncrement(typeOfCorals, typeOfCoral);
    });

    // set the new unique counts to the top-level ko properties
    coralWatchContainerToChartDataItems(self.data.colourCodeAverageRecordLevelCount, colourCodeAverages);
    coralWatchContainerToChartDataItems(self.data.typeOfCoralRecordLevelCount, typeOfCorals);
};

const coralWatchActivitySelfData = self.data;

var observationCounter = 0;
var Output_CoralWatch_coralObservationsRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    self.sampleId = ko.observable().extend({numericString:2});
    self.colourCodeLightest = ko.observable().extend({metadata:{metadata:self.dataModel['colourCodeLightest'], context:self.$context, config:config}});
    self.colourCodeDarkest = ko.observable().extend({metadata:{metadata:self.dataModel['colourCodeDarkest'], context:self.$context, config:config}});
    self.colourCodeAverage = ko.observable().extend({numericString:2});
    self.typeOfCoral = ko.observable().extend({metadata:{metadata:self.dataModel['typeOfCoral'], context:self.$context, config:config}});
    var coralSpeciesConfig = _.extend(config, {printable:'', dataFieldName:'coralSpecies', output: 'CoralWatch', surveyName: '' });
    self.coralSpecies = new SpeciesViewModel({}, coralSpeciesConfig);

    self.speciesPhoto = ko.observableArray([]);
    self.speciesPhoto = ko.observableArray([]);

    self.loadspeciesPhoto = function (data) {
        if (data !== undefined) {
            $.each(data, function (i, obj) {
                self.speciesPhoto.push(new ImageViewModel(obj, false, context));
            });
        }};

    self.loadData = function(data) {
        self['sampleId'](ecodata.forms.orDefault(data['sampleId'], ++observationCounter));
        self['colourCodeLightest'](ecodata.forms.orDefault(data['colourCodeLightest'], undefined));
        self['colourCodeDarkest'](ecodata.forms.orDefault(data['colourCodeDarkest'], undefined));
        self['colourCodeAverage'](ecodata.forms.orDefault(data['colourCodeAverage'], 0));
        self['typeOfCoral'](ecodata.forms.orDefault(data['typeOfCoral'], undefined));
        self['coralSpecies'].loadData(ecodata.forms.orDefault(data['coralSpecies'], {}));
        self.loadspeciesPhoto(ecodata.forms.orDefault(data['speciesPhoto'], []));
    };
    self.loadData(data || {});

    self.colourCodeLightest.subscribe(function(obj) {
        var lightCode = self.colourCodeLightest();
        if(lightCode) {
            var codes = ['B1','B2','B3','B4','B5','B6','C1','C2','C3','C4','C5','C6','D1','D2','D3','D4','D5','D6','E1','E2','E3','E4','E5','E6'];
            var lightCodeIndex = codes.indexOf(lightCode);
            var msg = [];
            $.each( codes, function( key, value ) {
                var codeNumber = lightCode.charAt(1);
                var integerVal = parseInt(value.charAt(1));
                if(integerVal >= codeNumber) {
                    msg.push({text: value, value: value});
                }
            });
            bootbox.prompt({
                title: "<h1>Select Dark colour code value<h1>",
                inputType: 'select',
                inputOptions: msg,
                buttons: {
                    confirm: {
                        label: 'OK',
                        className: 'btn-success'
                    },
                    cancel: {
                        label: 'Clear',
                        className: 'btn-danger'
                    }
                },
                callback: function (result) {
                    self.colourCodeDarkest(result);
                    if(self.colourCodeDarkest() && self.colourCodeLightest()) {
                        var total = parseInt(self.colourCodeDarkest().charAt(1)) + parseInt(self.colourCodeLightest().charAt(1));
                        if(total > 0) {
                            self.colourCodeAverage((total/2).toFixed(2));
                        }
                    }
                }
            });


        }
    });

    self.typeOfCoral.subscribe(function(obj) {
        if(!obj) {
            return;
        }
        var url = ''
        switch(obj) {
            case 'Plate corals':
                url = "https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&filename=plate.png&model=coralWatch";
                break;

            case 'Boulder corals':
                url = "https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&model=coralWatch&filename=boulder_layout.png";
                break;

            case 'Branching corals':
                url = "https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&model=coralWatch&filename=branching.png";
                break;

            case 'Soft corals':
                url = "https://biocollect.ala.org.au/download/getScriptFile?hub=coralWatch&model=coralWatch&filename=soft.png";
                break;

            default:
                url = "";
                break;
        }
        var msg = "<h2>Coral image not available!</h2>";
        if(url) {
            msg = "<img src='"+url+"'></img>";
        }
    });

    self.colourCodeAverage.subscribe(function (obj) {
        const currentCoralObservations = coralWatchActivitySelfData.coralObservations();
        coralWatchRecordLevelCountUpdate(currentCoralObservations);
    });

    self.typeOfCoral.subscribe(function (obj) {
        const currentCoralObservations = coralWatchActivitySelfData.coralObservations();
        coralWatchRecordLevelCountUpdate(currentCoralObservations);
    });
};

var context = _.extend({}, context, {parent:self, listName:'coralObservations'});
self.data.coralObservations = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_CoralWatch_coralObservationsRow, context:context, userAddedRows:true, config:config}});

self.data.coralObservations.subscribe(function (obj) {
    coralWatchRecordLevelCountUpdate(self.data.coralObservations());
});

self.data.coralObservations.loadDefaults = function() {
    self.data.coralObservations.addRow();
};

self.data.depthInMetres.subscribe(function(obj) {
    var feet =(parseFloat(self.data.depthInMetres()) * 3.28084);
    feet = feet.toFixed(2);
    self.data.depthInFeet(feet);
});

self.data.waterTemperatureInDegreesCelcius.subscribe(function(obj) {
    var farenheit = parseFloat((parseFloat(self.data.waterTemperatureInDegreesCelcius())).toFixed(2) * (9/5) + 32);
    farenheit = farenheit.toFixed(2);
    self.data.waterTemperatureInDegreesFarenheit(farenheit);
});

self.transients.convert = ko.computed(function() {
    if(self.data.waterTemperatureInDegreesFarenheit() == '0' && self.data.waterTemperatureInDegreesCelcius() == '0') {
        // Don't update
    } else {
        var celcius = (parseFloat(self.data.waterTemperatureInDegreesFarenheit()).toFixed(2) - 32) * (5/19);
        celcius = celcius.toFixed(2);
        self.data.waterTemperatureInDegreesCelcius(celcius);
    }
    var meters = (parseFloat(self.data.depthInFeet()).toFixed(2) / 3.28084);
    meters = meters.toFixed(2);
    self.data.depthInMetres(meters);

}, this);