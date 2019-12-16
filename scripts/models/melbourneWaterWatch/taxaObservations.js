
var Output_melbourneWaterWaterbugCensus_taxaObservationsRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});            var taxonNameConfig = _.extend(config, {printable:'', dataFieldName:'taxonName', output: 'Melbourne Water Waterbug Census', surveyName: '' });
    self.taxonName = new SpeciesViewModel({}, taxonNameConfig);

    self.taxonSensitivityRating = ko.observable().extend({numericString:2});

    self.individualCount = ko.observable().extend({numericString:2});

    self.taxonWeightFactor = ko.observable().extend({numericString:2});

    self.taxonIndexValue = ko.observable().extend({numericString:2});
    self.loadData = function(data) {
        self['taxonName'].loadData(ecodata.forms.orDefault(data['taxonName'], {}));
        self['taxonSensitivityRating'](ecodata.forms.orDefault(data['taxonSensitivityRating'], 0));
        self['individualCount'](ecodata.forms.orDefault(data['individualCount'], 0));
        self['taxonWeightFactor'](ecodata.forms.orDefault(data['taxonWeightFactor'], 0));
        self['taxonIndexValue'](ecodata.forms.orDefault(data['taxonIndexValue'], 0));
    };
    self.loadData(data || {});
};
var context = _.extend({}, context, {parent:self, listName:'taxaObservations'});
self.data.taxaObservations = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_melbourneWaterWaterbugCensus_taxaObservationsRow, context:context, userAddedRows:true, config:config}});

self.data.taxaObservations.loadDefaults = function() {
};


//Overide calculation.
self.transients.calculateSPI = function() {
    var sumOfIndexValues = 0;
    var sumOfWeights = 0;
    $.each(self.data.taxaObservations(), function (i, taxon) {
        if(taxon.taxonIndexValue() > 0)
            sumOfIndexValues = sumOfIndexValues + parseInt(taxon.taxonIndexValue());
        if(taxon.taxonWeightFactor() > 0)
            sumOfWeights = sumOfWeights + taxon.taxonWeightFactor();
    });

    var spi = sumOfWeights > 0 ? (sumOfIndexValues / sumOfWeights) : 0;
    self.data.spiValue(spi.toFixed(2));
    self.data.spiValue(spi);
    return spi;
};

self.transients.taxaRichness = function(){
    var taxaRichness = 0;
    $.each(self.data.taxaObservations(), function (i, taxon) {
        if(taxon.individualCount() > 0) {
            taxaRichness++;
        }
    });
    self.data.taxaRichness(taxaRichness);
    return taxaRichness;
};

self.transients.calculateStreamQualityRating = function(){
    var qualityRating = "";
    var spi = self.transients.calculateSPI();
    var richness = self.transients.taxaRichness();
    if(spi == 0 && richness == 0) {
        qualityRating = '';
    } else if (spi < 5.5) {
        if (richness <= 7) {
            qualityRating = "Poor";
        }
        else {
            qualityRating = "Good";
        }
    } else {
        if (richness <= 7) {
            qualityRating = "Fair";
        }
        else {
            qualityRating = "Excellent";
        };
    }
    self.data.streamQualityRating(qualityRating);
};
self.transients.updateHealth = function() {
    self.transients.taxaRichness();
    self.transients.calculateSPI();
    self.transients.calculateStreamQualityRating();
};


self.data.taxaObservations.subscribe(function(obj) {
    self.transients.updateHealth();
});

self.transients.dirtyItems = ko.computed(function() {
    self.transients.updateHealth();
    return ko.utils.arrayFilter(self.data.taxaObservations(), function(taxon) {
    });
}, this);