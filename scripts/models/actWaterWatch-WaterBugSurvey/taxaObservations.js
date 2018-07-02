// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/4948b263-ebad-4a98-883e-d0e8e2646d17
// Activity Name: ACT Water Watch - Water Bug Survey.
// Output model name: actWaterwatch_waterBugSurvey
// "jsMain": "https://dl.dropbox.com/s/qdv5zznw7grsuy1/taxaObservations.js?dl=0",
// https://www.dropbox.com/s/qdv5zznw7grsuy1/taxaObservations.js?dl=0
self.data.taxaObservations = ko.observableArray();
self.data.spiValue = ko.observable();
self.data.streamQualityRating = ko.observable();
self.transients.calculateSPI = function() {
	var sumOfIndexValues = 0;
	var sumOfWeights = 0;
	$.each(self.data.taxaObservations(), function (i, taxon) {
		sumOfIndexValues = sumOfIndexValues + taxon.taxonIndexValue();
		sumOfWeights = sumOfWeights + taxon.taxonWeightFactor();
	});
	var spi = sumOfWeights > 0 ? (sumOfIndexValues / sumOfWeights) : 0;
	spi = spi.toFixed(2);
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
	if (spi < 5.5) {
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
}
self.transients.updateHealth = function(){
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
		return taxon.transients.dirtyFlag.isDirty();
	});
}, this);

//Default behaviour
self.selectedtaxaObservationsRow = ko.observable();
self.loadtaxaObservations = function (data, append) {
	if (!append) {
		self.data.taxaObservations([]);
	}
	if (data === undefined) {
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Alderfly larva (Megaloptera)","dataType":"species","scientificName":"Megaloptera","taxonSensitivityRating":"8","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Sensitive"}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Backswimmer (Notonectidae)","dataType":"species","scientificName":"Notonectidae","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Beetle larva (Coleoptera)","dataType":"species","scientificName":"Coleoptera","taxonSensitivityRating":"5","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Biting Midge larva & pupa (Ceratopogonidae)","dataType":"species","scientificName":"Ceratopogonidae","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Bloodworm, Non-biting Midge (Chironomidae)","dataType":"species","scientificName":"Chironomidae","taxonSensitivityRating":"1","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Caddisfly larva (Trichoptera)","dataType":"species","scientificName":"Trichoptera","taxonSensitivityRating":"8","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Sensitive","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Damselfly nymph (Odonata - Zygoptera)","dataType":"species","scientificName":"Odonata - Zygoptera","taxonSensitivityRating":"6","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Diving Beetle & larva (Dytiscidae)","dataType":"species","scientificName":"Dytiscidae","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Dragonfly nymph (Odonata - Epiprocta)","dataType":"species","scientificName":"Odonata - Epiprocta","taxonSensitivityRating":"4","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Fly larva & pupa (Diptera)","dataType":"species","scientificName":"Diptera","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Free-living Flatworm, Flatworms  (Turbellaria)","dataType":"species","scientificName":"Turbellaria","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Hydra (Hydrozoa)","dataType":"species","scientificName":"Hydrozoa","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Mussel (Bivalvia)","dataType":"species","scientificName":"Bivalvia","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Sandhopper (Amphipoda)","dataType":"species","scientificName":"Amphipoda","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Segmented worm  (Oligochaeta)","dataType":"species","scientificName":"Oligochaeta","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Shrimp (Caridea)","dataType":"species","scientificName":"Caridea","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Slater, Isopod  (Isopoda)","dataType":"species","scientificName":"Isopoda","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Snail (Gastropoda)","dataType":"species","scientificName":"Gastropoda","taxonSensitivityRating":"1","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Freshwater Yabby/Crayfish (Decapoda)","dataType":"species","scientificName":"Decapoda","taxonSensitivityRating":"4","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Giant Water Bug  (Belostomatidae)","dataType":"species","scientificName":"Belostomatidae","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Leech (Hirudinea)","dataType":"species","scientificName":"Hirudinea","taxonSensitivityRating":"1","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Mayfly nymph (Ephemeroptera)","dataType":"species","scientificName":"Ephemeroptera","taxonSensitivityRating":"9","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very sensitive","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Mosquito larva & pupa (Culicidae)","dataType":"species","scientificName":"Culicidae","taxonSensitivityRating":"1","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Nematode (Nematoda)","dataType":"species","scientificName":"Nematoda","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Riffle Beetle & larva (Elmidae)","dataType":"species","scientificName":"Elmidae","taxonSensitivityRating":"7","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Sensitive","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Stonefly nymph (Plecoptera)","dataType":"species","scientificName":"Plecoptera","taxonSensitivityRating":"10","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very sensitive","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Water Boatman (Corixidae)","dataType":"species","scientificName":"Corixidae","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Water Mites (Arcarina)","dataType":"species","scientificName":"Arcarina","taxonSensitivityRating":"6","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Sensitive","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Water Scorpions/Needle Bug  (Nepidae)","dataType":"species","scientificName":"Nepidae","taxonSensitivityRating":"3","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Water Strider (Gerridae)","dataType":"species","scientificName":"Gerridae","taxonSensitivityRating":"4","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Water Treader (Mesoveliidae)","dataType":"species","scientificName":"Mesoveliidae","taxonSensitivityRating":"2","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Very tolerant","taxonIndexValue":""}));
		self.data.taxaObservations.push(new TaxaObservationsRow({"taxonName":"Whirligig Beetle & larva (Gyrinidae)","dataType":"species","scientificName":"Gyrinidae","taxonSensitivityRating":"4","taxonWeightFactor":"","individualCount":"","dwcAttribute":"scientificName","taxonSensitivityClass":"Tolerant","taxonIndexValue":""}));
	} else {
		$.each(data, function (i, obj) {
			self.data.taxaObservations.push(new TaxaObservationsRow(obj));
		});
	}
};
self.addtaxaObservationsRow = function () {
	var newRow = new TaxaObservationsRow();
	self.data.taxaObservations.push(newRow);
};
self.removetaxaObservationsRow = function (row) {
	self.data.taxaObservations.remove(row);
};
self.taxaObservationsrowCount = function () {
	return self.data.taxaObservations().length;
};

