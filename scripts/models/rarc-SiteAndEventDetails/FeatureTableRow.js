// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsMain: https://dl.dropbox.com/s/bx89xwobzp8yerq/FeatureTableRow.js?dl=0
// Minified: https://dl.dropbox.com/s/cjqrn6rt6602lgx/FeatureTableRow.min.js?dl=0
var Output_rarc_EventAndLocationDetails_featuresTableRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});
    self.feature_parameter = ko.observable();

    self.feature_transectOne = ko.observable().extend({metadata:{metadata:self.dataModel['feature_transectOne'], context:context, config:config}});

    self.feature_transectTwo = ko.observable().extend({metadata:{metadata:self.dataModel['feature_transectTwo'], context:context, config:config}});

    self.feature_transectThree = ko.observable().extend({metadata:{metadata:self.dataModel['feature_transectThree'], context:context, config:config}});

    self.feature_transectFour = ko.observable().extend({metadata:{metadata:self.dataModel['feature_transectFour'], context:context, config:config}});

    self.featuresAverageScore = ko.observable();
    self.loadData = function(data) {
        self['feature_parameter'](ecodata.forms.orDefault(data['feature_parameter'], undefined));
        self['feature_transectOne'](ecodata.forms.orDefault(data['feature_transectOne'], undefined));
        self['feature_transectTwo'](ecodata.forms.orDefault(data['feature_transectTwo'], undefined));
        self['feature_transectThree'](ecodata.forms.orDefault(data['feature_transectThree'], undefined));
        self['feature_transectFour'](ecodata.forms.orDefault(data['feature_transectFour'], undefined));
        self['featuresAverageScore'](ecodata.forms.orDefault(data['featuresAverageScore'], undefined));
    };
    self.loadData(data || {});

    // Custom
    self.transients.convertToInt = function(parameter, value) {
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

    self.transients.featuresAverageScore = ko.computed(function() {
        var one, two, three, four, count = 0, total = 0;
        one = self.transients.convertToInt(self.feature_parameter(), self.feature_transectOne());
        two = self.transients.convertToInt(self.feature_parameter(), self.feature_transectTwo());
        three = self.transients.convertToInt(self.feature_parameter(), self.feature_transectThree());
        four = self.transients.convertToInt(self.feature_parameter(), self.feature_transectFour());
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
        self.featuresAverageScore(average.toFixed(2));
        return average;
    }, this);


};
var context = _.extend({}, context, {parent:self, listName:'featuresTable'});
self.data.featuresTable = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_rarc_EventAndLocationDetails_featuresTableRow, context:context, userAddedRows:false, config:config}});

self.data.featuresTable.loadDefaults = function() {
    self.data.featuresTable.addRow({"nativeCanopyRegen_T3":"","nativeCanopyRegen_T4":"","nativeCanopyRegen_averageScore":"","feature_parameter":"Native canopy species regeneration","nativeCanopyRegen_T1":"","nativeCanopyRegen_T2":""});
    self.data.featuresTable.addRow({"nativeUnderstoreyRegen_averageScore":"","feature_parameter":"Native understorey regeneration","nativeUnderstoreyRegen_T1":"","nativeUnderstoreyRegen_T2":"","nativeUnderstoreyRegen_T3":"","nativeUnderstoreyRegen_T4":""});
    self.data.featuresTable.addRow({"tussockGrasses_averageScore":"","feature_parameter":"Large native tussock grasses","tussockGrasses_T4":"","tussockGrasses_T3":"","tussockGrasses_T2":"","tussockGrasses_T1":""});
    self.data.featuresTable.addRow({"reeds_T1":"","reeds_T4":"","feature_parameter":"Reeds","reeds_T3":"","reeds_T2":"","reeds_averageScore":""});
};
