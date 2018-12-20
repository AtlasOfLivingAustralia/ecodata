// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsMain: https://dl.dropbox.com/s/b3vxokkwb3ntgfe/DebrisTableRow.js?dl=0
// Minified: https://dl.dropbox.com/s/d6tr2lqnvd0jcuf/DebrisTableRow.min.js?dl=0
// START
var Output_rarc_EventAndLocationDetails_debrisTableRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    self.debris_parameter = ko.observable();

    self.debris_transectOne = ko.observable().extend({metadata:{metadata:self.dataModel['debris_transectOne'], context:context, config:config}});

    self.debris_transectTwo = ko.observable().extend({metadata:{metadata:self.dataModel['debris_transectTwo'], context:context, config:config}});

    self.debris_transectThree = ko.observable().extend({metadata:{metadata:self.dataModel['debris_transectThree'], context:context, config:config}});

    self.debris_transectFour = ko.observable().extend({metadata:{metadata:self.dataModel['debris_transectFour'], context:context, config:config}});

    self.vegetationWidthAverageScore = ko.observable();
    self.loadData = function(data) {
        self['debris_parameter'](ecodata.forms.orDefault(data['debris_parameter'], undefined));
        self['debris_transectOne'](ecodata.forms.orDefault(data['debris_transectOne'], undefined));
        self['debris_transectTwo'](ecodata.forms.orDefault(data['debris_transectTwo'], undefined));
        self['debris_transectThree'](ecodata.forms.orDefault(data['debris_transectThree'], undefined));
        self['debris_transectFour'](ecodata.forms.orDefault(data['debris_transectFour'], undefined));
        self['vegetationWidthAverageScore'](ecodata.forms.orDefault(data['vegetationWidthAverageScore'], 0));
    };
    self.loadData(data || {});

    // Custom script.
    var parameters = ['Total leaf litter','Native leaf litter', 'Standing dead trees', 'Hollow bearing trees (>20cm DBH)', 'Fallen logs (>10cm diameter)'];
    self.transients.convertToInt = function (parameter, value) {
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

    self.transients.vegetationWidthAverageScore = ko.computed(function () {
        var one, two, three, four, count = 0, total = 0;
        one = self.transients.convertToInt(self.debris_parameter(), self.debris_transectOne());
        two = self.transients.convertToInt(self.debris_parameter(), self.debris_transectTwo());
        three = self.transients.convertToInt(self.debris_parameter(), self.debris_transectThree());
        four = self.transients.convertToInt(self.debris_parameter(), self.debris_transectFour());
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
        self.vegetationWidthAverageScore(average.toFixed(2));
        return average;
    }, this);

};

var context = _.extend({}, context, {parent:self, listName:'debrisTable'});
self.data.debrisTable = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_rarc_EventAndLocationDetails_debrisTableRow, context:context, userAddedRows:false, config:config}});
self.data.debrisTable.loadDefaults = function() {
    self.data.debrisTable.addRow({"debris_parameter":"Total leaf litter","totalLeafLitter_T4":"","totalLeafLitter_T3":"","totalLeafLitter_T2":"","totalLeafLitter_T1":"","totalLeafLitter_averageScore":""});
    self.data.debrisTable.addRow({"debris_parameter":"Native leaf litter","nativeLeafLitter_T1":"","nativeLeafLitter_T3":"","nativeLeafLitter_T2":"","nativeLeafLitter_T4":"","nativeLeafLitter_averageScore":""});
    self.data.debrisTable.addRow({"deadTrees_averageScore":"","debris_parameter":"Standing dead trees","deadTrees_T3":"","deadTrees_T2":"","deadTrees_T4":"","deadTrees_T1":""});
    self.data.debrisTable.addRow({"debris_parameter":"Hollow bearing trees (>20cm DBH)","hollowBearingTrees_T1":"","hollowBearingTrees_averageScore":"","hollowBearingTrees_T2":"","hollowBearingTrees_T3":"","hollowBearingTrees_T4":""});
    self.data.debrisTable.addRow({"debris_parameter":"Fallen logs (>10cm diameter)","logs_averageScoreTotal":"","logs_T3_score":"","logs_T1_score":"","logs_T4_score":"","noEdit":true,"logs_T2_score":""});
};

// END