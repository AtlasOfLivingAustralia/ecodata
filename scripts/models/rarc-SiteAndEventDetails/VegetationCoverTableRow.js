// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsClass: https://dl.dropbox.com/s/9h8sh2wnzj0n4xy/VegetationCoverTableRow.js?dl=0
var VegetationCoverTableRow = function (data) {
    var self = this;
    if (!data) data = {};
    this.transients = {};
    this.vegetationCover_parameter = ko.observable(orBlank(data['vegetationCover_parameter']));

    var parameters = ['Total canopy %', 'Native canopy %', 'Total understorey %', 'Native understorey %', 'Total ground cover %', 'Native ground cover %', 'No. of structural layers'];
    var constraint1 = ["None","1 - 30%","31 - 60%","> 60%"];
    var constraint2 = ["None","1 - 5%","6 - 30%","> 30%"];
    var constraint3 = ["0","1","2","3"];
    var constraint  = constraint1;
    if(this.vegetationCover_parameter() ==  parameters[2] || this.vegetationCover_parameter() == parameters[3] ) {
        constraint = constraint2;
    } else if(this.vegetationCover_parameter() ==  parameters[6]) {
        constraint = constraint3;
    }

    this.vegetationCover_transectOne = ko.observable(orBlank(data['vegetationCover_transectOne']));
    this.transients.vegetationCover_transectOneConstraints = constraint;
    this.vegetationCover_transectTwo = ko.observable(orBlank(data['vegetationCover_transectTwo']));
    this.transients.vegetationCover_transectTwoConstraints = constraint;
    this.vegetationCover_transectThree = ko.observable(orBlank(data['vegetationCover_transectThree']));
    this.transients.vegetationCover_transectThreeConstraints = constraint;
    this.vegetationCover_transectFour = ko.observable(orBlank(data['vegetationCover_transectFour']));
    this.transients.vegetationCover_transectFourConstraints = constraint;
    this.vegetationCoverAverageScore = ko.observable(orZero(data['vegetationCoverAverageScore']));

    this.transients.convertToInt = function(value) {
        var intValue = '';
        switch(value) {
            case "None" :
                intValue  = 0;
                break;
            case "1 - 5%":
            case "1 - 30%":
                intValue  = 1;
                break;
            case "6 - 30%":
            case "31 - 60%":
                intValue  = 2;
                break;
            case "> 30%":
            case "> 60%":
                intValue  = 3;
                break;
            case "0":
                intValue  = 0;
                break;
            case "1":
                intValue  = 1;
                break;
            case "2":
                intValue  = 2;
                break;
            case "3":
                intValue  = 3;
                break;
            default:

        }
        return intValue;
    };

    this.transients.vegetationWidthAverageScore = ko.computed(function() {
        var one, two, three, four, count = 0, total = 0;
        one = this.transients.convertToInt(this.vegetationCover_transectOne());
        two = this.transients.convertToInt(this.vegetationCover_transectTwo());
        three = this.transients.convertToInt(this.vegetationCover_transectThree());
        four = this.transients.convertToInt(this.vegetationCover_transectFour());
        if(one){total = total + one; count++;}
        if(two){total = total + two; count++;}
        if(three){total = total + three; count++;}
        if(four){total = total + four; count++;}
        var average = ((total)/count);
        average = isNaN(average) ? 0 : average;
        this.vegetationCoverAverageScore(average.toFixed(2));
        return average;
    }, this);

    this.transients.dirtyFlag = new ko.dirtyFlag(this);
};
