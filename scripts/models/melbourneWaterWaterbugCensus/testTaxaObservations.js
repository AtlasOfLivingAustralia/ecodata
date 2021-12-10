var Output_melbourneWaterWaterbugCensus_taxaObservationsRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    self.taxonName = ko.observable();
    // self.taxonSensitivityClass = ko.observable();
    self.taxonSensitivityRating = ko.observable();
    self.individualCount = ko.observable();
    self.taxonWeightFactor = ko.observable();
    self.taxonIndexValue = ko.observable();

    self.transients = {};
    self.individualCount.subscribe(function(changed){
        console.log(self.individualCount())
        console.log(changed)
    });

    self.taxonWeightFactor = ko.computed(function() {
        if (self.taxonName() == "hello") {
            console.log('have taxon name')
        }
        return 20;
    })

    self.taxonName.subscribe(function(changed) {
        console.log(changed)
    }, this);

    self.loadDefaults = function() {};

    self.loadData = function(data) {
        console.log(self['taxonName'](ecodata.forms.orDefault(data['taxonName'], undefined)));
        self['taxonName'](ecodata.forms.orDefault(data['taxonName'], undefined));
        // self['taxonSensitivityClass'](ecodata.forms.orDefault(data['taxonSensitivityClass'], undefined));
        self['taxonSensitivityRating'](ecodata.forms.orDefault(data['taxonSensitivityRating'], 0));
        self['individualCount'](ecodata.forms.orDefault(data['individualCount'], 0));
        //self['taxonWeightFactor'](ecodata.forms.orDefault(data['taxonWeightFactor'], 0));
        self['taxonIndexValue'](ecodata.forms.orDefault(data['taxonIndexValue'], ''));
    };
    self.loadData(data || {});
};


//
var context = _.extend({}, context, {parent:self, listName:'taxaObservations'});
self.data.taxaObservations = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_melbourneWaterWaterbugCensus_taxaObservationsRow, context:context, userAddedRows:true, config:config}});
//
self.data.taxaObservations.loadDefaults = function() {
//
};
//
// console.log(self.data.taxaObservations)