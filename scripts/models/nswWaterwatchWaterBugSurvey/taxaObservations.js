var Output_melbourneWaterWaterbugCensus_taxaObservationsRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    var taxonNameConfig = _.extend(config, {printable:'', dataFieldName:'taxonName', output: 'Melbourne Water Waterbug Census', surveyName: '' });

    self.taxonName = new SpeciesViewModel({}, taxonNameConfig);

    self.taxonSensitivityRating = ko.observable()//.extend({numericString:2});

    self.individualCount = ko.observable()//.extend({numericString:2});

    self.taxonWeightFactor = ko.observable()//.extend({numericString:2});

    self.taxonIndexValue = ko.observable()//.extend({numericString:2});

    self.transients = {};

    self.individualCount.subscribe(function(changed){

    });

    var sensitivityRatings = {
        "TRICHOPTERA":"8",
        "Acari":"6",
        "COLEOPTERA":"5",
        "ODONATA":"4",
        "GERRIDAE":"4",
        "GYRINIDAE":"4",
        "ZYGOPTERA":"3",
        "DIPTERA":"3",
        "CERATOPOGONIDAE":"3",
        "NEMATODA":"3",
        "CARIDEA":"3",
        "DYTISCIDAE":"2",
        "Turbellaria":"2",
        "ISOPODA":"2",
        "CORIXIDAE":"2",
        "OLIGOCHAETA":"2",
        "NOTONECTIDAE":"1",
        "CHIRONOMIDAE":"1",
        "HIRUDINEA":"1",
        "CULICIDAE":"1",
        "GASTROPODA":"1",
        "EPHEMEROPTERA":"9",
        "PLECOPTERA":"10",
        "NEPIDAE":"3",
        "MEGALOPTERA":"8",
        "AMPHIPODA":"3",
        "BIVALVIA":"3",
        "MESOVELIIDAE":"2",
        "DECAPODA":"4",
        "HYDROZOA":"2",
        "ELMIDAE":"7",
        "Trichoptera":"8",
        "Acari":"6",
        "Coleoptera":"5",
        "Odonata":"4",
        "Gerridae":"4",
        "Gyrinidae":"4",
        "Zygoptera":"3",
        "Diptera":"3",
        "Ceratopogonidae":"3",
        "Nematoda":"3",
        "Caridea":"3",
        "Dytiscidae":"2",
        "Turbellaria":"2",
        "Isopoda":"2",
        "Corixidae":"2",
        "Oligichaete Worms":"2",
        "Notonectidae":"1",
        "Chironomidae":"1",
        "Hirudinea":"1",
        "Culicidae":"1",
        "Gastropoda":"1",
        "Ephemeroptera":"9",
        "Plecoptera":"10",
        "Nepidae":"3",
        "Megaloptera":"8",
        "Amphipoda":"3",
        "Bivalvia":"3",
        "Mesoveliidae":"2",
        "Decapoda":"4",
        "Hydrozoa":"2",
        "Elmidae":"7",
        "Belostomatidae":""
    }

    // other way to update sensitivity rating
    self.taxonSensitivityRating = ko.computed(function() {

        var scientificName = self.taxonName.scientificName();
        console.log(scientificName)
        console.log(self.taxonName)
        if (scientificName in sensitivityRatings) {
            return sensitivityRatings[scientificName];
        }
        else {
            return 0;
        }

    },this);

    self.taxonWeightFactor = ko.computed(function() {
        var weight = 0;
        self.individualCount(Math.round(self.individualCount()));
        var bugCount = self.individualCount();

        var weight = 0;
        if (bugCount > 20) {
            weight = 5;
        } else if (bugCount > 10) {
            weight = 4;
        } else if (bugCount > 5) {
            weight = 3;
        } else if (bugCount > 2) {
            weight = 2;
        } else if (bugCount > 0) {
            weight = 1;
        } else {
            weight = 0;
        }
        var indexValue = weight *  self.taxonSensitivityRating();
        self.taxonIndexValue(indexValue);
        return weight;
    }, this);


    self.loadData = function(data) {
        self['taxonName'].loadData(ecodata.forms.orDefault(data['taxonName'], {}));
        // self['taxonSensitivityRating'](ecodata.forms.orDefault(data['taxonSensitivityRating'], 0));
        self['individualCount'](ecodata.forms.orDefault(data['individualCount'], 0));
        // self['taxonWeightFactor'](ecodata.forms.orDefault(data['taxonWeightFactor'], 0));
        self['taxonIndexValue'](ecodata.forms.orDefault(data['taxonIndexValue'], 0));
    };
    self.loadData(data || {});
};
var context = _.extend({}, context, {parent:self, listName:'taxaObservations'});
self.data.taxaObservations = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_melbourneWaterWaterbugCensus_taxaObservationsRow, context:context, userAddedRows:true, config:config}});

self.data.taxaObservations.loadDefaults = function() {
};


//Override calculation.

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
    // act version
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
    // melbourne version
    // var qualityRating = "";
    // if (spi < 3.0) {
    //   qualityRating = "Poor";
    // }
    // else if (spi < 4.0) {
    //   qualityRating = "Fair";
    // }
    // else if (spi < 6.0) {
    //   qualityRating = "Good";
    // }
    // else {
    //   qualityRating = "Excellent";
    // }
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