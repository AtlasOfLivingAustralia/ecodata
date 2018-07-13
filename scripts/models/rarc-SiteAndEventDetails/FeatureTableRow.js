// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsClass: https://dl.dropbox.com/s/bx89xwobzp8yerq/FeatureTableRow.js?dl=0
// Minified: https://dl.dropbox.com/s/cjqrn6rt6602lgx/FeatureTableRow.min.js?dl=0
var FeaturesTableRow = function (data) {
    var self = this;
    if (!data) data = {};
    this.transients = {};
    var parameters = ['Native canopy species regeneration','Native understorey regeneration','Large native tussock grasses','Reeds'];
    var constraint1 = ["None","Scattered","Abundant with grazing damage","Abundant"];
    var constraint2 = ["None","Scattered","Abundant"];
    var constraint  = constraint1;

    this.feature_parameter = ko.observable(orBlank(data['feature_parameter']));
    if(this.feature_parameter() == 'Reeds' || this.feature_parameter() == 'Large native tussock grasses') {
        constraint = constraint2;
    }
    this.feature_transectOne = ko.observable(orBlank(data['feature_transectOne']));
    this.transients.feature_transectOneConstraints = constraint;
    this.feature_transectTwo = ko.observable(orBlank(data['feature_transectTwo']));
    this.transients.feature_transectTwoConstraints = constraint;
    this.feature_transectThree = ko.observable(orBlank(data['feature_transectThree']));
    this.transients.feature_transectThreeConstraints = constraint;
    this.feature_transectFour = ko.observable(orBlank(data['feature_transectFour']));
    this.transients.feature_transectFourConstraints = constraint;
    this.featuresAverageScore = ko.observable(orZero(data['featuresAverageScore']));

    this.transients.convertToInt = function(parameter, value) {
        var intValue = '';
        if(parameter == 'Reeds' || parameter == 'Large native tussock grasses') {
            switch(value) {
                case "None" :
                    intValue  = 0;
                    break;
                case "Scattered":
                    intValue  = 1;
                    break;
                case "Abundant":
                    intValue  = 2;
                    break;
                default:
            }
        } else  {
            switch(value) {
                case "None" :
                    intValue  = 0;
                    break;
                case "Scattered":
                    intValue  = 1;
                    break;
                case "Abundant":
                    intValue  = 2;
                    break;
                case "Abundant with grazing damage":
                    intValue = -0.5;
                default:
            }
        }
        return intValue;
    };

    //Rules: Regeneration 􏰀 1 m tall: 0 = none, 1 = scattered, and 2 = abundant, with 1/2 point subtracted for grazing damage Reeds and large tussock grasses: 0 = none, 1 = scattered, and 2 = abundant
    this.transients.featuresAverageScore = ko.computed(function() {
        var one, two, three, four, count = 0, total = 0;
        one = this.transients.convertToInt(this.feature_parameter(), this.feature_transectOne());
        two = this.transients.convertToInt(this.feature_parameter(), this.feature_transectTwo());
        three = this.transients.convertToInt(this.feature_parameter(), this.feature_transectThree());
        four = this.transients.convertToInt(this.feature_parameter(), this.feature_transectFour());
        if (one) {
            total = total + one;
            count++;
        }
        if (two) {
            total = total + two;
            count++;
        }
        if (three) {
            total = total + three;
            count++;
        }
        if (four) {
            total = total + four;
            count++;
        }
        var average = ((total) / count);
        average = isNaN(average) ? 0 : average;
        this.featuresAverageScore(average.toFixed(2));
        return average;
    }, this);
    this.transients.dirtyFlag = new ko.dirtyFlag(this);
};