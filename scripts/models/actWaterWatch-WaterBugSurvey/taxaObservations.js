// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/4948b263-ebad-4a98-883e-d0e8e2646d17
// Activity Name: ACT Water Watch - Water Bug Survey.
// Output model name: actWaterwatch_waterBugSurvey
// "jsMain": "https://dl.dropbox.com/s/qdv5zznw7grsuy1/taxaObservations.js?dl=0",
// https://dl.dropbox.com/s/hxtk06ythysu4xr/taxaObservations.min.js?dl=0

var Output_ACT_Waterwatch_Modified_SIGNAL2_macroinvertebrates_taxaObservationsRow = function (data, dataModel, context, config) {
	var self = this;
	ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
	context = _.extend(context, {parent:self});
	self.taxonName = ko.observable();
	self.taxonSensitivityClass = ko.observable();
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
		self['taxonSensitivityClass'](ecodata.forms.orDefault(data['taxonSensitivityClass'], undefined));
		self['taxonSensitivityRating'](ecodata.forms.orDefault(data['taxonSensitivityRating'], 0));
		self['individualCount'](ecodata.forms.orDefault(data['individualCount'], 0));
		//self['taxonWeightFactor'](ecodata.forms.orDefault(data['taxonWeightFactor'], 0));
		self['taxonIndexValue'](ecodata.forms.orDefault(data['taxonIndexValue'], ''));
	};
	self.loadData(data || {});
};

var context = _.extend({}, context, {parent:self, listName:'taxaObservations'});
self.data.taxaObservations = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_ACT_Waterwatch_Modified_SIGNAL2_macroinvertebrates_taxaObservationsRow, context:context, userAddedRows:false, config:config}});

self.data.taxaObservations.loadDefaults = function() {
    self.data.taxaObservations.addRow({"scientificName":"Mecoptera","taxonSensitivityClass":"Very Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Scorpion flies","scientificName":"Mecoptera","name":"Scorpion flies (Mecoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:67c46151-7449-407a-a8b3-a283ba3f0771"},"dwcAttribute":"scientificName","taxonSensitivityRating":"10"});
    self.data.taxaObservations.addRow({"scientificName":"Plecoptera","taxonSensitivityClass":"Very Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Stoneflies","scientificName":"Plecoptera","name":"Stoneflies (Plecoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:4fbe14c4-2efb-4874-8842-f29e05a93f92"},"dwcAttribute":"scientificName","taxonSensitivityRating":"10"});
    self.data.taxaObservations.addRow({"scientificName":"Ephemeroptera","taxonSensitivityClass":"Very Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"May flies","scientificName":"Ephemeroptera","name":"May flies (Ephemeroptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:928c5312-17a2-4557-b523-d207cacc332b"},"dwcAttribute":"scientificName","taxonSensitivityRating":"9"});
    self.data.taxaObservations.addRow({"scientificName":"Megaloptera","taxonSensitivityClass":"Very Sensitive","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Alder flies","scientificName":"Megaloptera","name":"Alder flies (Megaloptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:1764aba8-641d-4eb8-ade5-ff33efafb054"},"dwcAttribute":"scientificName","taxonSensitivityRating":"8"});
    self.data.taxaObservations.addRow({"scientificName":"Trichoptera","taxonSensitivityClass":"Very Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Caddis flies","scientificName":"Trichoptera","name":"Caddis flies (Trichoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:0964bf51-f620-4a71-9ab3-ff631f2099bb"},"dwcAttribute":"scientificName","taxonSensitivityRating":"8"});
    self.data.taxaObservations.addRow({"scientificName":"Nematomorpha","taxonSensitivityClass":"Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Horsehair worms; gordian worms","scientificName":"Nematomorpha","name":"Horsehair worms; gordian worms (Nematomorpha)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:4b1dd080-6a02-48c4-9f6c-94680f7651dd"},"dwcAttribute":"scientificName","taxonSensitivityRating":"6"});
    self.data.taxaObservations.addRow({"scientificName":"Acari","taxonSensitivityClass":"Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Mites","scientificName":"Acarina","name":"Mites (Acarina)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:c731c9bb-6292-4071-873d-2e8543dd120f"},"dwcAttribute":"scientificName","taxonSensitivityRating":"6"});
    self.data.taxaObservations.addRow({"scientificName":"Anaspidacea","taxonSensitivityClass":"Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Cave shrimp","scientificName":"Anaspidacea","name":"Cave shrimp (Anaspidacea)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:5b4de720-d042-47b3-824a-542b12e7c771"},"dwcAttribute":"scientificName","taxonSensitivityRating":"6"});
    self.data.taxaObservations.addRow({"scientificName":"Neuroptera","taxonSensitivityClass":"Sensitive","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Lacewings","scientificName":"Neuroptera","name":"Lacewings (Neuroptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:db09c273-56ae-4ef9-a5ed-53027aa7c63e"},"dwcAttribute":"scientificName","taxonSensitivityRating":"6"});
    self.data.taxaObservations.addRow({"scientificName":"Coleoptera","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Beetles - Riffle beetles, Whirligigs","scientificName":"Coleoptera","name":"Beetles - Riffle beetles, Whirligigs (Coleoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:5c387616-0cb4-42f0-936e-7ec22d576939"},"dwcAttribute":"scientificName","taxonSensitivityRating":"5"});
    self.data.taxaObservations.addRow({"scientificName":"Porifera","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater sponges","scientificName":"Porifera","name":"Freshwater sponges (Porifera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:ed334702-b153-41b0-ac93-e6aa4964331c"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Bryozoa","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Pipe-mosses","scientificName":"Bryozoa","name":"Pipe-mosses (Bryozoa)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:a1f069f9-eaa8-487c-889a-d3cfb3dd936e"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Decapoda","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Yabbies; crayfish, shrimp","scientificName":"Decapoda","name":"Yabbies; crayfish, shrimp (Decapoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:2f12112b-d593-4392-a9db-4b026b8805a3"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Diplopoda","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Aquatic millipedes","scientificName":"Diplopoda","name":"Aquatic millipedes (Diplopoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:0a08c6cb-7990-4124-ac83-9d44274d6a84"},"dwcAttribute":"scientificName","taxonSensitivityRating":"4"});
    self.data.taxaObservations.addRow({"scientificName":"Nemertea","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Proboscis worms","scientificName":"Nemertea","name":"Proboscis worms (Nemertea)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:89e92ab7-7ffc-4cc4-9149-19c8f8079940"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Nematoda","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Nematodes, roundworms","scientificName":"Nematoda","name":"Nematodes, roundworms (Nematoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:0e7e0f7d-4456-495b-b762-2d11f78b9368"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Bivalvia","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater mussels; clams","scientificName":"Bivalvia","name":"Freshwater mussels; clams (Bivalvia)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:8c3070f6-9475-4b6a-95cb-8afb944ad3d5"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Amphipoda","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Side-swimmers; scuds","scientificName":"Amphipoda","name":"Side-swimmers; scuds (Amphipoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:c799e373-f43a-446d-b2d0-836e6be97b84"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Diptera","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Fly larva - mosquito larvae, bloodworms","scientificName":"Diptera","name":"Fly larva - mosquito larvae, bloodworms (Diptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:933b2bf6-deee-4fd9-b669-4bf8cf7cc9ce"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Odonata","taxonSensitivityClass":"Moderately Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Dragonfly; damselflies","scientificName":"Odonata","name":"Dragonfly; damselflies (Odonata)","guid":"NZOR-4-24409"},"dwcAttribute":"scientificName","taxonSensitivityRating":"3"});
    self.data.taxaObservations.addRow({"scientificName":"Turbellaria","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Flatworms","scientificName":"Turbellaria","name":"Flatworms (Turbellaria)","guid":"13010000"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Oligochaeta","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Segmented worms","scientificName":"Oligochaeta","name":"Segmented worms (Oligochaeta)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:406916d5-9058-4d72-9dbf-d3f689e8f3b2"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Isopoda","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Slaters","scientificName":"Isopoda","name":"Freshwater Slaters (Isopoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:e4720a22-d642-44c7-abc6-fb5b34d5e057"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Hemiptera","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"True bugs - backswimmers, water boatman, needle bugs","scientificName":"Hemiptera","name":"True bugs - backswimmers, water boatman, needle bugs (Hemiptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:7630fe33-a00e-4743-80da-4fa6a36cd8b2"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Lepidoptera","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Moth larvae","scientificName":"Lepidoptera","name":"Moth larvae (Lepidoptera)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:7cb6c81c-a7c4-4dd5-8578-fcfd2de847d6"},"dwcAttribute":"scientificName","taxonSensitivityRating":"2"});
    self.data.taxaObservations.addRow({"scientificName":"Hydrozoa","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Hydras","scientificName":"Hydrozoa","name":"Hydras (Hydrozoa)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:40e34a43-accb-48e3-9492-09c39ac5f756"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Gastropoda","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Freshwater Snails","scientificName":"Gastropoda","name":"Freshwater Snails (Gastropoda)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:ab81c7fc-3fc3-4e54-b277-a12a1a9cd0d8"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Hirudinea","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Leeches","scientificName":"Hirudinea","name":"Leeches (Hirudinea)","guid":"22300000"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Polychaeta","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Bristleworms","scientificName":"Polychaeta","name":"Bristleworms (Polychaeta)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:d1251470-e6f7-4f43-b97d-276dab41b06b"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Anostraca","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Brine shrimps; fairy shrimps","scientificName":"Anostraca","name":"Brine shrimps; fairy shrimps (Anostraca)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:dbc4f4ad-0ad5-4813-9275-95b00b448832"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Branchiura","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Fish lice","scientificName":"Branchiura","name":"Fish lice (Branchiura)","guid":"NZOR-4-111042"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Cyclestheriidae","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Clam shrimps","scientificName":"Cyclestheriidae","name":"Clam shrimps (Cyclestheriidae)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:925a4c2a-19fe-43c9-af4c-9b420b85b13a"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Notostraca","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Tadpole shrimp","scientificName":"Notostraca","name":"Tadpole shrimp (Notostraca)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:7d0c7db7-6e86-4c63-bb4c-ca80c1b84a06"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
    self.data.taxaObservations.addRow({"scientificName":"Collembola","taxonSensitivityClass":"Very Tolerant","taxonIndexValue":"","dataType":"species","taxonWeightFactor":"","individualCount":"","taxonName":{"commonName":"Springtails","scientificName":"Collembola","name":"Springtails (Collembola)","guid":"urn:lsid:biodiversity.org.au:afd.taxon:53e5e456-0d08-4cff-ac1f-d453b2c07e3b"},"dwcAttribute":"scientificName","taxonSensitivityRating":"1"});
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