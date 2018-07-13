// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/4948b263-ebad-4a98-883e-d0e8e2646d17
// Activity Name: ACT Water Watch - Water Bug Survey.
// Output model name: actWaterwatch_waterBugSurvey
// jsClass: https://dl.dropbox.com/s/ilp2dw0un4ej782/TaxaObservationsRow.js?dl=0
// Minified: https://dl.dropbox.com/s/30fnaw0udvnini8/TaxaObservationsRow.min.js?dl=0
var TaxaObservationsRow = function (data, vm) {
    var self = this;
    if (!data) data = {};
    this.transients = {};
    this.taxonName = ko.observable(orBlank(data['taxonName']));
    this.taxonSensitivityClass = ko.observable(orBlank(data['taxonSensitivityClass']));
    this.taxonSensitivityRating = ko.observable(orZero(data['taxonSensitivityRating']));
    this.individualCount = ko.observable(orZero(data['individualCount']));
    this.taxonIndexValue = ko.observable(orZero(data['taxonIndexValue']));
    this.individualCount.subscribe(function(changed){
    });
    this.taxonWeightFactor = ko.computed(function() {
        var weight = 0;
        var bugCount = this.individualCount();

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
		var indexValue = weight *  this.taxonSensitivityRating();
        this.taxonIndexValue(indexValue);
        return weight;
    }, this);
	
	this.transients.dirtyFlag = new ko.dirtyFlag(this);
};
