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
        "Aranea":"5",
        "Acarina":"5",
        "Sphaeriacea":"4",
        "Potamopyrgus antipodarum":"3",
        "Amphipoda":"3",
        "Elmidae":"7",
        "Elmidae":"7",
        "Scirtidae":"6",
        "Byrrocryptus":"10",
        "Athericidae":"8",
        "Diptera":"3",
        "Simuliidae":"5",
        "Chironomidae":"4",
        "Chironomidae":"4",
        "Tipulidae":"5",
        "Baetidae":"5",
        "Leptophlebiidae":"8",
        "Heteroptera":"1",
        "Telephlebiidae":"9",
        "Gripopterygidae":"9",
        "Hydropsychidae":"6",
        "Hydrobiosidae":"8",
        "Oecetis":"2",
        "Caenota plicata":"9",
        "Leptoceridae":"3",
        "Triplectides":"10",
        "Notalina":"6",
        "Oligochaeta":"2",
        "Ancylidae":"5",
        "Lymnaeidae":"1",
        "Physa acuta":"2",
        "Atyidae":"3",
        "Caenidae":"4",
        "Gerridae":"4",
        "Micronecta":"3",
        "Anisoptera":"4",
        "Hydroptilidae":"4",
        "Hirudinea":"1",
        "Turbellaria":"2",
        'Araneus': "5",
        "Araneidae": "5",
        'Acari': "5",
        'Acarina': "5",
        'SPHAERIIDAE':'4',
        "Potamopyrgus antipodarum": "3",
        "AMPHIPODA":"3",
        "ELMIDAE":"7",
        "SCIRTIDAE":"6",
        "Byrrocryptus":"10",
        "ATHERICIDAE":"8",
        "DIPTERA":"3",
        "Simuliidae":"5",
        "CHIRONOMIDAE":"4",
        "TIPULIDAE":"5",
        "BAETIDAE":"5",
        "LEPTOPHLEBIIDAE":"8",
        "HETEROPTERA":"1",
        "TELEPHLEBIIDAE":"9",
        "GRIPOPTERYGIDAE":"9",
        "HYDROPSYCHIDAE":"6",
        "HYDROBIOSIDAE":"8",
        "Elmidae": "7",
        "Oecetis":"2",
        "Caenota plicata":"9",
        "LEPTOCERIDAE":"3",
        "Triplectides":"10",
        "Notalina":"6",
        "OLIGOCHAETA":"2",
        "ANCYLIDAE":"5",
        "LYMNAEIDAE":"1",
        "Physa acuta":"2",
        "ATYIDAE":"3",
        "CAENIDAE":"4",
        "GERRIDAE":"4",
        "Micronecta":"3",
        "ANISOPTERA":"4",
        "HYDROPTILIDAE":"4",
        "Triplectides":"6",
        "HIRUDINEA":"1",
        "Turbellaria":"2",
        "HYMENOSOMATIDAE":"3",
        "CERATOPOGONIDAE":"4",
        "ZYGOPTERA":"1",
        "Anisocentropus":"7",
        "HYDROPHILIDAE":"2",
        "DYTISCIDAE":"3",
        "Enithares":"3",
        "ZYGOPTERA":"2",
        "Ecnomus":"6",
        "Sigara":"4",
        "MEGAPODAGRIONIDAE":"5",
        "AESHNIDAE":"4",
        "HYRIIDAE":"5",
        "GYRINIDAE":"4",
        "Anisops":"2",
        "GOMPHIDAE":"5",
        "PLANORBIDAE":"2",
        "Brachycera":"1",
        "Sclerocyphon":"6",
        "DIXIDAE":"7",
        "SYNLESTIDAE":"7",
        "JANIRIDAE":"3",
        "CULICIDAE":"1",
        "Atalophlebia":"10",
        "ISOSTICTIDAE":"3",
        "DYTISCIDAE":"4",
        "PARASTACIDAE":"1",
        "NOTONEMOURIDAE":"6",
        "Illiesoperla":"6",
        "Atriplectides":"7",
        "CORYDALIDAE":"10",
        "PARASTACIDAE":"8",
        "ANNULIPALPIA":"2",
        "Lectrides varians":"4",
        "COLEOPTERA":"6",
        "Oecetis":"5",
        "DYTISCIDAE":"1",
        "PLEIDAE":"2",
        "Symphitoneuria":"6",
        "GYRINIDAE":"4",
        "Agapetus":"9",
        "AUSTROPERLIDAE":"10",
        "DYTISCIDAE":"1",
        "STRATIOMYIDAE":"2",
        "NEMATOMORPHA":"6",
        "Coloburiscoides":"8",
        "Tasmanophlebia":"8",
        "Helicopsyche":"8",
        "DYTISCIDAE":"6",
        "Rhantus":"1",
        "DYTISCIDAE":"6",
        "HYDROPHILIDAE":"2",
        "Helochares":"2",
        "Diaprepocoris barycephalus":"1",
        "Lancetes":"2",
        "Jappa":"8",
        "DYTISCIDAE":"5",
        "GRIPOPTERYGIDAE":"10",
        "Riekoperla":"10",
        "Agraptocorixa":"5",
        "HYDROMETRIDAE":"3",
        "EUSTHENIIDAE":"10",
        "Laccotrephes":"4",
        "LEPTOCEROIDEA":"8",
        "SERICOSTOMATOIDEA":"8",
        "Symbiocladius":"8",
        "Acruroperla atra":"6",
        "Australphilus":"3",
        "NEMATODA":"3",
        "Siphlonuridae":"10",
        "Eretes australis":"1",
        "Ranatra":"2",
        "Aranea":"5",
        "Acarina":"5",
        "Sphaeriacea":"4",
        "Potamopyrgus antipodarum":"3",
        "Amphipoda":"3",
        "Elmidae":"7",
        "Elmidae":"7",
        "Scirtidae":"6",
        "Byrrocryptus":"10",
        "Athericidae":"8",
        "Diptera":"3",
        "Simuliidae":"5",
        "Chironomidae":"4",
        "Tipulidae":"5",
        "Baetidae":"5",
        "Leptophlebiidae":"8",
        "Heteroptera":"1",
        "Telephlebiidae":"9",
        "Gripopterygidae":"9",
        "Hydropsychidae":"6",
        "Hydrobiosidae":"8",
        "Oecetis":"2",
        "Caenota plicata":"9",
        "Leptoceridae":"3",
        "Triplectides":"10",
        "Notalina":"6",
        "Oligochaeta":"2",
        "Ancylidae":"5",
        "Lymnaeidae":"1",
        "Physa acuta":"2",
        "Atyidae":"3",
        "Caenidae":"4",
        "Gerridae":"4",
        "Micronecta":"3",
        "Anisoptera":"4",
        "Hydroptilidae":"4",
        "Hirudinea":"1",
        "Turbellaria":"2",
        "Hymenosomatidae":"3",
        "Ceratopogonidae":"4",
        "Anisocentropus":"7",
        "Hydrophilidae":"2",
        "Dytiscidae":"3",
        "Enithares":"3",
        "Zygoptera":"2",
        "Ecnomus":"6",
        "Sigara":"4",
        "Megapodagrionidae":"5",
        "Aeshnidae":"4",
        "Hyriidae":"5",
        "Gyrinidae":"4",
        "Anisops":"2",
        "Gomphidae":"5",
        "Planorbidae":"2",
        "Brachycera":"1",
        "Sclerocyphon":"6",
        "Dixidae":"7",
        "Synlestidae":"7",
        "Janiridae":"3",
        "Culicidae":"1",
        "Atalophlebia":"10",
        "Isostictidae":"3",
        "Dytiscidae":"4",
        "Parastacidae":"1",
        "Notonemouridae":"6",
        "Illiesoperla":"6",
        "Atriplectides":"7",
        "Planorbidae":"1",
        "Corydalidae":"10",
        "Parastacidae":"8",
        "Annulipalpia":"2",
        "Lectrides varians":"4",
        "Coleoptera":"6",
        "Oecetis":"5",
        "Dytiscidae":"1",
        "Pleidae":"2",
        "Symphitoneuria":"6",
        "Gyrinidae":"4",
        "Agapetus":"9",
        "Austroperlidae":"10",
        "Dytiscidae":"1",
        "Stratiomyidae":"2",
        "Nematomorpha":"6",
        "Coloburiscoides":"8",
        "Tasmanophlebia":"8",
        "Helicopsyche":"8",
        "Dytiscidae":"6",
        "Rhantus":"1",
        "Dytiscidae":"6",
        "Hydrophilidae":"2",
        "Helochares":"2",
        "Diaprepocoris barycephala":"1",
        "Lancetes":"2",
        "Jappa":"8",
        "Dytiscidae":"5",
        "Gripopterygidae":"10",
        "Riekoperla":"10",
        "Agraptocorixa Kirkaldy, 1898":"5",
        "Hydrometridae":"3",
        "Eustheniidae":"10",
        "Laccotrephes":"4",
        "Leptoceroidea":"8",
        "Sericostomatoidea":"8",
        "Symbiocladius":"8",
        "Acruroperla atra":"6",
        "Australphilus":"3",
        "Nematoda":"3",
        "Siphlonuridae":"10",
        "Eretes":"1",
        "Ranatra Fabricius, 1790":"2"
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