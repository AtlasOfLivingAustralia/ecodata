// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsClass: https://dl.dropbox.com/s/b3vxokkwb3ntgfe/DebrisTableRow.js?dl=0
// Minified: https://dl.dropbox.com/s/d6tr2lqnvd0jcuf/DebrisTableRow.min.js?dl=0
var DebrisTableRow = function (data) {
    var self = this;
    if (!data) data = {};
    this.transients = {};
    this.debris_parameter = ko.observable(orBlank(data['debris_parameter']));

    var constraint1 = ["None", "1 - 30%", "31 - 60%", "> 60%"]; // Total Leaf and Native leaf.
    var constraint2 = ["Absent", "Present"]; // Standing dead, Hallow beaering
    var constraint3 = ["None", "Small quantities", "Abundant"]; // Fallen logs.
    var constraint = constraint1;
    var parameters = ['Total leaf litter', 'Native leaf litter', 'Standing dead trees', 'Hollow bearing trees (>20cm DBH)', 'Fallen logs (>10cm diameter)'];

    if (this.debris_parameter() == parameters[0] || this.debris_parameter() == parameters[1]) {
        constraint = constraint1;
    } else if (this.debris_parameter() == parameters[2] || this.debris_parameter() == parameters[3]) {
        constraint = constraint2;
    } else if (this.debris_parameter() == parameters[4]) {
        constraint = constraint3;
    }

    this.debris_transectOne = ko.observable(orBlank(data['debris_transectOne']));
    this.transients.debris_transectOneConstraints = constraint;
    this.debris_transectTwo = ko.observable(orBlank(data['debris_transectTwo']));
    this.transients.debris_transectTwoConstraints = constraint;
    this.debris_transectThree = ko.observable(orBlank(data['debris_transectThree']));
    this.transients.debris_transectThreeConstraints = constraint;
    this.debris_transectFour = ko.observable(orBlank(data['debris_transectFour']));
    this.transients.debris_transectFourConstraints = constraint;
    this.vegetationWidthAverageScore = ko.observable(orZero(data['vegetationWidthAverageScore']));

    this.transients.convertToInt = function (parameter, value) {
        var intValue = '';
        //Leaf litter and native leaf litter cover: 0 = none, 1 = 1–30%, 2 = 31–60%, 3 = 􏰃 60%
        if (parameter == parameters[0] || parameter == parameters[1]) {
            switch (value) {
                case "None" :
                    intValue = 0;
                    break;
                case "1 - 30%":
                    intValue = 1;
                    break;
                case "31 - 60%":
                    intValue = 2;
                    break;
                case "> 60%":
                    intValue = 3;
                    break;
                default:
            }
        }
        //Standing dead trees ( 􏰃 20 cm dbh) and hollow-bearing trees: 0 = absent, 1 = present
        else if (parameter == parameters[2] || parameter == parameters[3]) {
            switch (value) {
                case "Absent" :
                    intValue = 0;
                    break;
                case "Present":
                    intValue = 1;
                    break;
                default:
            }
        }
        //Fallen logs ( 􏰃 10 cm diameter): 0 = none, 1 = small quantities, 2 = abundant
        else if (parameter == parameters[4]) {
            switch (value) {
                case "None" :
                    intValue = 0;
                    break;
                case "Small quantities":
                    intValue = 1;
                    break;
                case "Abundant" :
                    intValue = 2;
                    break;
                default:
            }
        }
        return intValue;
    };
    this.transients.vegetationWidthAverageScore = ko.computed(function () {
        var one, two, three, four, count = 0, total = 0;
        one = this.transients.convertToInt(this.debris_parameter(), this.debris_transectOne());
        two = this.transients.convertToInt(this.debris_parameter(), this.debris_transectTwo());
        three = this.transients.convertToInt(this.debris_parameter(), this.debris_transectThree());
        four = this.transients.convertToInt(this.debris_parameter(), this.debris_transectFour());
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
        var average = (total / count);
        average = isNaN(average) ? 0 : average;
        this.vegetationWidthAverageScore(average.toFixed(2));
        return average;
    }, this);

    this.transients.dirtyFlag = new ko.dirtyFlag(this);
};
