// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/4948b263-ebad-4a98-883e-d0e8e2646d17
// Activity Name: ACT Water Watch - Water Bug Survey.
// Output model name: actWaterwatch_waterBugSurvey
// "jsMain": "https://dl.dropbox.com/s/qdv5zznw7grsuy1/taxaObservations.js?dl=0",
// https://dl.dropbox.com/s/hxtk06ythysu4xr/taxaObservations.min.js?dl=0

var Output_NSW_Waterwatch_Modified_SIGNAL2_macroinvertebrates_taxaObservationsRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    self.taxonName = ko.observable();
    self.taxonSensitivityRating = ko.observable();
    self.individualCount = ko.observable();
    self.taxonWeightFactor = ko.observable();
    self.taxonIndexValue = ko.observable();


    self.transients = {};
    self.individualCount.subscribe(function(changed){
    });
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
        self['taxonName'](ecodata.forms.orDefault(data['taxonName'], undefined));
        self['taxonSensitivityRating'](ecodata.forms.orDefault(data['taxonSensitivityRating'], 0));
        self['individualCount'](ecodata.forms.orDefault(data['individualCount'], 0));
        //self['taxonWeightFactor'](ecodata.forms.orDefault(data['taxonWeightFactor'], 0));
        self['taxonIndexValue'](ecodata.forms.orDefault(data['taxonIndexValue'], ''));
    };
    self.loadData(data || {});
};

var context = _.extend({}, context, {parent:self, listName:'taxaObservations'});
self.data.taxaObservations = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_NSW_Waterwatch_Modified_SIGNAL2_macroinvertebrates_taxaObservationsRow, context:context, userAddedRows:false, config:config}});

self.data.taxaObservations.loadDefaults = function() {
    self.data.taxaObservations.addRow({"scientificName":"Trichoptera","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Caddisfly larva","scientificName":"Trichoptera","name":"Caddisfly larva (Trichoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:5f714393-00e3-44c1-a864-6e13a7771e32"},"dwcAttribute":"scientificName","taxonSensitivityRating":"8"});
    self.data.taxaObservations.addRow({"scientificName":"Acari","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Water Mites","scientificName":"Acarina","name":"Mites (Acarina)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:c731c9bb-6292-4071-873d-2e8543dd120f"},"dwcAttribute":"scientificName","taxonSensitivityRating":"6"});
    self.data.taxaObservations.addRow({"scientificName":"Coleoptera","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Beetle larva","scientificName":"Coleoptera","name":"Beetle larva (Coleoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:5c387616-0cb4-42f0-936e-7ec22d576939"},"dwcAttribute":"scientificName","taxonSensitivityRating":"5"});
    self.data.taxaObservations.addRow({"scientificName":"Odonata","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Dragonfly nymph","scientificName":"Odonata","name":"Dragonfly nymph (Odonata - Epiprocta)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:31e391dc-8eb4-4bd0-88ac-96b1499d0f56"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Gerridae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Water Strider","scientificName":"Gerridae","name":"Water Strider (Gerridae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:1a5f5d0d-6c6b-4f4d-be44-e378671eb29e"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Gyrinidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Whirligig Beetle & larva","scientificName":"Gyrinidae","name":"Whirligig Beetle & larva (Gyrinidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:14a6ebf0-0889-4040-b8d4-c7c1bd304d95"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Zygoptera","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Damselfly nymph","scientificName":"Zygoptera","name":"Damselfly nymph (Odonata - Zygoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:672ca4f8-ace8-4283-8253-1019c8d86e63"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Diptera","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Fly larva & pupa","scientificName":"Diptera","name":"Fly larva & pupa (Diptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:933b2bf6-deee-4fd9-b669-4bf8cf7cc9ce"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Ceratopogonidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Biting Midge larva & pupa","scientificName":"Ceratopogonidae","name":"Biting Midge larva & pupa (Ceratopogonidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:612702ab-f6b2-4558-9aa0-9ae8fd71e9d1"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Nematoda","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Nematodes","scientificName":"Nematoda","name":"Nematodes (Nematoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:0e7e0f7d-4456-495b-b762-2d11f78b9368"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Caridea","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Shrimp","scientificName":"Caridea","name":"Freshwater Shrimp (Caridea)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:3af1d3b9-f856-4e3e-894b-f32bd48108fc"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Dytiscidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Diving Beetle & larva","scientificName":"Dytiscidae","name":"Diving Beetle & larva (Dytiscidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:82bb8470-faa0-4030-9b8f-bb3af4337d5b"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Turbellaria","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Free-living Flatworm, Flatworms","scientificName":"Turbellaria","name":"Free-living Flatworm, Flatworms (Turbellaria)","guid":"13010000"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Isopoda","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Slaters","scientificName":"Isopoda","name":"Freshwater Slaters (Isopoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:e4720a22-d642-44c7-abc6-fb5b34d5e057"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Corixidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Water Boatman","scientificName":"Corixidae","name":"Water Boatman (Corixidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:40f82aa8-435b-45b6-8398-2974fddd6a4d"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Oligochaeta","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater segmented worm","scientificName":"Oligochaeta","name":"Freshwater segmented worm (Oligochaeta)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:406916d5-9058-4d72-9dbf-d3f689e8f3b2"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Notonectidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Backswimmer","scientificName":"Notonectidae","name":"Backswimmer (Notonectidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:8903f26f-3a12-46b9-913c-5d5e326ab104"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Chironomidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Bloodworm, Non-biting Midge","scientificName":"Chironomidae","name":"Bloodworm, Non-biting Midge (Chironomidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:ed04029e-d352-4c7a-b7d6-b23c583edde8"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Hirudinea","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Leeches","scientificName":"Hirudinea","name":"Leeches (Hirudinea)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:da5c0009-21c6-47f6-ac5c-45ba52494e4e"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Culicidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Mosquito larva & pupa","scientificName":"Culicidae","name":"Mosquito larva & pupa (Culicidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:6fc026dd-eac1-42b8-98a5-b019a7977209"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Gastropoda","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Snails","scientificName":"Gastropoda","name":"Freshwater Snails (Gastropoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:ab81c7fc-3fc3-4e54-b277-a12a1a9cd0d8"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Ephemeroptera","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Mayfly nymph","scientificName":"Ephemeroptera","name":"Mayfly nymph (Ephemeroptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:928c5312-17a2-4557-b523-d207cacc332b"},"dwcAttribute":"scientificName","taxonSensitivityRating":"9"});
    self.data.taxaObservations.addRow({"scientificName":"Plecoptera","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Stonefly nymph","scientificName":"Plecoptera","name":"Stonefly nymph (Plecoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:4fbe14c4-2efb-4874-8842-f29e05a93f92"},"dwcAttribute":"scientificName","taxonSensitivityRating":"10"});
    self.data.taxaObservations.addRow({"scientificName":"Nepidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Water Scorpions/Needle Bug","scientificName":"Nepidae","name":"Water Scorpions/Needle Bug (Nepidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:5839a034-111e-4adc-b5b9-b8090799cc72"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Megaloptera","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Alderfly larva","scientificName":"Megaloptera","name":"Alderfly larva (Megaloptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:1764aba8-641d-4eb8-ade5-ff33efafb054"},"dwcAttribute":"scientificName","taxonSensitivityRating":"8"});
    self.data.taxaObservations.addRow({"scientificName":"Amphipoda","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Sandhopper","scientificName":"Amphipoda","name":"Freshwater Sandhopper (Amphipoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:2db0708e-716a-4c18-9898-57774ab32d57"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Bivalvia","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater mussels","scientificName":"Bivalvia","name":"Freshwater mussels (Bivalvia)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:d6ab0629-da3d-4e6a-8400-301515cb3f16"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Mesoveliidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Water Treader","scientificName":"Mesoveliidae","name":"Water Treader (Mesoveliidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:8a5a37e5-7d5a-4f47-be24-ba544a104f4e"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Decapoda","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Yabby/Crayfish","scientificName":"Decapoda","name":"Freshwater Yabby/Crayfish (Decapoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:2f12112b-d593-4392-a9db-4b026b8805a3"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Hydrozoa","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Hydra","scientificName":"Hydrozoa","name":"Freshwater Hydra (Hydrozoa)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:ebb1ea2c-6624-4c6e-acd6-f4faf34a88aa"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Elmidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Riffle Beetle & larva","scientificName":"Elmidae","name":"Riffle Beetle & larva (Elmidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:265b66c1-947c-4e30-87fc-f21781179688"},"dwcAttribute":"scientificName","taxonSensitivityRating":"7"});
    self.data.taxaObservations.addRow({"scientificName":"Belostomatidae","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Giant Water Bug","scientificName":"Belostomatidae","name":"Giant Water Bug (Belostomatidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:08c11223-0d9e-4025-897f-fea3728f786b"},"dwcAttribute":"scientificName","taxonSensitivityRating":"7"});

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