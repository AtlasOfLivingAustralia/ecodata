// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsClass: https://dl.dropbox.com/s/mvhqohl0u2xwsj4/CanopyVegetationTableRow.js?dl=0
var CanopyVegetationTableRow = function (data) {
    var self = this;
    if (!data) data = {};
    this.transients = {};
    this.parameter = ko.observable(orBlank(data['parameter']));
    this.transectOne = ko.observable(orZero(data['transectOne']));
    this.transectTwo = ko.observable(orZero(data['transectTwo']));
    this.transectThree = ko.observable(orZero(data['transectThree']));
    this.transectFour = ko.observable(orZero(data['transectFour']));
    this.vegetationWidthAverageScore = ko.observable(orZero(data['vegetationWidthAverageScore']));
    this.transients.dirtyFlag = new ko.dirtyFlag(this);
};



