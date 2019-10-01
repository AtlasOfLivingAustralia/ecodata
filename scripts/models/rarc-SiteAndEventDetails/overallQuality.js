// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsMain: https://dl.dropbox.com/s/8tesww79r304hyj/overallQuality.js?dl=0
// Minified: https://dl.dropbox.com/s/svbt8rur6lr3z89/overallQuality.min.js?dl=0

// CanopyVegetation
var Output_rarc_EventAndLocationDetails_canopyVegetationTableRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent: self});
    self.parameter = ko.observable();
    self.transectOne = ko.observable().extend({numericString: 2});
    self.transectOne.enableConstraint = ko.computed(function () {
        var condition = 'parameter != "Score"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.transectTwo = ko.observable().extend({numericString: 2});
    self.transectTwo.enableConstraint = ko.computed(function () {
        var condition = 'parameter != "Score"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.transectThree = ko.observable().extend({numericString: 2});
    self.transectThree.enableConstraint = ko.computed(function () {
        var condition = 'parameter != "Score"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.transectFour = ko.observable().extend({numericString: 2});
    self.transectFour.enableConstraint = ko.computed(function () {
        var condition = 'parameter != "Score"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.vegetationWidthAverageScore = ko.observable();
    self.loadData = function (data) {
        self['parameter'](ecodata.forms.orDefault(data['parameter'], undefined));
        self['transectOne'](ecodata.forms.orDefault(data['transectOne'], 0));
        self['transectTwo'](ecodata.forms.orDefault(data['transectTwo'], 0));
        self['transectThree'](ecodata.forms.orDefault(data['transectThree'], 0));
        self['transectFour'](ecodata.forms.orDefault(data['transectFour'], 0));
        self['vegetationWidthAverageScore'](ecodata.forms.orDefault(data['vegetationWidthAverageScore'], ''));
    };
    self.loadData(data || {});
};

var context = _.extend({}, context, {parent: self, listName: 'canopyVegetationTable'});
self.data.canopyVegetationTable = ko.observableArray().extend({
    list: {
        metadata: self.dataModel,
        constructorFunction: Output_rarc_EventAndLocationDetails_canopyVegetationTableRow,
        context: context,
        userAddedRows: false,
        config: config
    }
});

self.data.canopyVegetationTable.loadDefaults = function () {
    self.data.canopyVegetationTable.addRow({
        "channelWidth_T1": "",
        "channelWidth_averageScore": "",
        "channelWidth_T2": "",
        "channelWidth_T3": "",
        "channelWidth_T4": "",
        "parameter": "Channel Width (CW)"
    });
    self.data.canopyVegetationTable.addRow({
        "vegetationWidth_T2": "",
        "vegetationWidth_T1": "",
        "vegetationWidth_T4": "",
        "parameter": "Vegetation Width (VW)",
        "vegetationWidth_T3": "",
        "vegetationWidth_averageScore": ""
    });
    self.data.canopyVegetationTable.addRow({
        "vegetationWidth_T2_score": "",
        "vegetationWidth_T1_score": "",
        "vegetationWidth_averageScoreTotal": "",
        "vegetationWidth_T3_score": "",
        "parameter": "Score",
        "vegetationWidth_T4_score": ""
    });
};

self.transients.tempCanopyVegetationTableScore = ko.observable();
self.transients.canopyVegetationTableScore = function (cw, vw) {
    if (!cw && !vw) return "";
    cw = parseInt(cw);
    vw = parseInt(vw);
    if (isNaN(cw) || isNaN(vw)) return 0;
    if (cw <= 10) {
        if (vw < 5) return 0;
        if (vw < 10) return 1;
        if (vw < 20) return 2;
        return vw < 40 ? 3 : 4;
    }
    vw /= cw;
    if (vw < 0.5) return 0;
    if (vw < 1) return 1;
    if (vw < 2) return 2;
    return vw < 4 ? 3 : 4;
};

self.transients.updateCanopyVegetationScore = function () {
    var cwRow = ko.utils.arrayFirst(self.data.canopyVegetationTable(), function (row) {
        return row.parameter() == "Channel Width (CW)";
    });
    var vwRow = ko.utils.arrayFirst(self.data.canopyVegetationTable(), function (row) {
        return row.parameter() == "Vegetation Width (VW)";
    });
    $.each(self.data.canopyVegetationTable(), function (i, canopy) {
        if (canopy.parameter() == 'Score') {
            canopy.transectOne(self.transients.canopyVegetationTableScore(cwRow.transectOne(), vwRow.transectOne()));
            canopy.transectTwo(self.transients.canopyVegetationTableScore(cwRow.transectTwo(), vwRow.transectTwo()));
            canopy.transectThree(self.transients.canopyVegetationTableScore(cwRow.transectThree(), vwRow.transectThree()));
            canopy.transectFour(self.transients.canopyVegetationTableScore(cwRow.transectFour(), vwRow.transectFour()));
            var averageScore = ((+canopy.transectOne() + +canopy.transectTwo() + +canopy.transectThree() + +canopy.transectFour()) / 4).toFixed(2);
            canopy.vegetationWidthAverageScore(averageScore);
            self.transients.tempCanopyVegetationTableScore(averageScore)
        } else {
            canopy.vegetationWidthAverageScore("");
            self.transients.tempCanopyVegetationTableScore(0)
        }
    });
};

self.data.canopyVegetationTable.subscribe(function (obj) {
    self.transients.updateCanopyVegetationScore();
});
self.transients.canopyVegetationTableDirtyItems = ko.computed(function () {
    self.transients.updateCanopyVegetationScore();
}, this);

// Vegetation
var Output_rarc_EventAndLocationDetails_vegetationCoverTableRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent: self});
    self.vegetationCover_parameter = ko.observable();

    self.vegetationCover_transectOne = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['vegetationCover_transectOne'],
            context: context,
            config: config
        }
    });
    self.vegetationCover_transectOne.enableConstraint = ko.computed(function () {
        var condition = 'vegetationCover_parameter != "No. of structural layers"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.vegetationCover_transectTwo = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['vegetationCover_transectTwo'],
            context: context,
            config: config
        }
    });
    self.vegetationCover_transectTwo.enableConstraint = ko.computed(function () {
        var condition = 'vegetationCover_parameter != "No. of structural layers"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.vegetationCover_transectThree = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['vegetationCover_transectThree'],
            context: context,
            config: config
        }
    });
    self.vegetationCover_transectThree.enableConstraint = ko.computed(function () {
        var condition = 'vegetationCover_parameter != "No. of structural layers"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });
    self.vegetationCover_transectFour = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['vegetationCover_transectFour'],
            context: context,
            config: config
        }
    });
    self.vegetationCover_transectFour.enableConstraint = ko.computed(function () {
        var condition = 'vegetationCover_parameter != "No. of structural layers"';
        return ecodata.forms.expressionEvaluator.evaluateBoolean(condition, self);
    });

    self.vegetationCoverAverageScore = ko.observable().extend({numericString: 2});
    self.loadData = function (data) {
        self['vegetationCover_parameter'](ecodata.forms.orDefault(data['vegetationCover_parameter'], undefined));
        self['vegetationCover_transectOne'](ecodata.forms.orDefault(data['vegetationCover_transectOne'], undefined));
        self['vegetationCover_transectTwo'](ecodata.forms.orDefault(data['vegetationCover_transectTwo'], undefined));
        self['vegetationCover_transectThree'](ecodata.forms.orDefault(data['vegetationCover_transectThree'], undefined));
        self['vegetationCover_transectFour'](ecodata.forms.orDefault(data['vegetationCover_transectFour'], undefined));
        self['vegetationCoverAverageScore'](ecodata.forms.orDefault(data['vegetationCoverAverageScore'], undefined));
    };
    self.loadData(data || {});
};
var context = _.extend({}, context, {parent: self, listName: 'vegetationCoverTable'});
self.data.vegetationCoverTable = ko.observableArray().extend({
    list: {
        metadata: self.dataModel,
        constructorFunction: Output_rarc_EventAndLocationDetails_vegetationCoverTableRow,
        context: context,
        userAddedRows: false,
        config: config
    }
});

self.data.vegetationCoverTable.loadDefaults = function () {
    self.data.vegetationCoverTable.addRow({
        "vegetationCover_parameter": "Total canopy %",
        "totalCanopy_T3": "",
        "totalCanopy_T4": "",
        "totalCanopy_T1": "",
        "totalCanopy_T2": "",
        "totalCanopy_averageScore": ""
    });
    self.data.vegetationCoverTable.addRow({
        "vegetationCover_parameter": "Native canopy %",
        "nativeCanopy_T4": "",
        "nativeCanopy_T2": "",
        "nativeCanopy_averageScore": "",
        "nativeCanopy_T3": "",
        "nativeCanopy_T1": ""
    });
    self.data.vegetationCoverTable.addRow({
        "totalUnderstorey_T3": "",
        "totalUnderstorey_T4": "",
        "vegetationCover_parameter": "Total understorey %",
        "totalUnderstorey_T1": "",
        "totalUnderstorey_T2": "",
        "totalUnderstorey_averageScore": ""
    });
    self.data.vegetationCoverTable.addRow({
        "vegetationCover_parameter": "Native understorey %",
        "nativeUnderstorey_T1": "",
        "nativeUnderstorey_T4": "",
        "nativeUnderstorey_T3": "",
        "nativeUnderstorey_T2": "",
        "nativeUnderstorey_averageScore": ""
    });
    self.data.vegetationCoverTable.addRow({
        "vegetationCover_parameter": "Total ground cover %",
        "totalGroundCover_T3_score": "",
        "totalGroundCover_T2_score": "",
        "totalGroundCover_T1_score": "",
        "totalGroundCover_averageScoreTotal": "",
        "noEdit": true,
        "totalGroundCover_T4_score": ""
    });
    self.data.vegetationCoverTable.addRow({
        "nativeGroundCover_T1_score": "",
        "nativeGroundCover_T4_score": "",
        "vegetationCover_parameter": "Native ground cover %",
        "nativeGroundCover_averageScoreTotal": "",
        "noEdit": true,
        "nativeGroundCover_T3_score": "",
        "nativeGroundCover_T2_score": ""
    });
    self.data.vegetationCoverTable.addRow({
        "vegetationCover_parameter": "No. of structural layers",
        "numberOfLayers_T1_score": "",
        "numberOfLayers_T3_score": "",
        "noEdit": true,
        "numberOfLayers_T4_score": "",
        "numberOfLayers_averageScoreTotal": "",
        "numberOfLayers_T2_score": ""
    });
};

self.transients.layerRow = function (layerName) {
    var row = ko.utils.arrayFirst(self.data.vegetationCoverTable(), function (row) {
        return row.vegetationCover_parameter() == layerName;
    });

    return row;
};
self.transients.calculateLayers = function (col, param1, param2) {
    var value = 0;
    var p1 = self.transients.layerRow(param1);
    var p2 = self.transients.layerRow(param2);
    var allowed = ["1 - 30%", "31 - 60%", "> 60%", "1 - 5%", "6 - 30%", "> 30%"];
    if (col == 0 && p1 && p2 && ($.inArray(p1.vegetationCover_transectOne(), allowed) != -1 || $.inArray(p2.vegetationCover_transectOne(), allowed) != -1 )) {
        value = 1;
    } else if (col == 1 && p1 && p2 && ($.inArray(p1.vegetationCover_transectTwo(), allowed) != -1 || $.inArray(p2.vegetationCover_transectTwo(), allowed) != -1)) {
        value = 1;
    } else if (col == 2 && p1 && p2 && ($.inArray(p1.vegetationCover_transectThree(), allowed) != -1 || $.inArray(p2.vegetationCover_transectThree(), allowed) != -1)) {
        value = 1;
    } else if (col == 3 && p1 && p2 && ($.inArray(p1.vegetationCover_transectFour(), allowed) != -1 || $.inArray(p2.vegetationCover_transectFour(), allowed) != -1)) {
        value = 1;
    }
    return value;
};

self.transients.convertToVegInt = function (value) {
    var intValue = '';
    switch (value) {
        case "" : // Nothings selected
            intValue = -1;
            break;
        case "None" :
            intValue = 0;
            break;
        case "1 - 5%":
        case "1 - 30%":
            intValue = 1;
            break;
        case "6 - 30%":
        case "31 - 60%":
            intValue = 2;
            break;
        case "> 30%":
        case "> 60%":
            intValue = 3;
            break;
        case "0":
            intValue = 0;
            break;
        case "1":
            intValue = 1;
            break;
        case "2":
            intValue = 2;
            break;
        case "3":
            intValue = 3;
            break;
        default:
            intValue = -1;
            break;

    }
    return intValue;
};


self.transients.updateVegetationCoverTableLayers = function () {
    $.each(self.data.vegetationCoverTable(), function (i, row) {
        if (row.vegetationCover_parameter() != 'No. of structural layers') {
            var one, two, three, four, count = 0, total = 0;
            one = self.transients.convertToVegInt(row.vegetationCover_transectOne());
            two = self.transients.convertToVegInt(row.vegetationCover_transectTwo());
            three = self.transients.convertToVegInt(row.vegetationCover_transectThree());
            four = self.transients.convertToVegInt(row.vegetationCover_transectFour());
            if (one >= 0) {
                total = total + one;
                count++;
            }
            if (two >= 0) {
                total = total + two;
                count++;
            }
            if (three >= 0) {
                total = total + three;
                count++;
            }
            if (four >= 0) {
                total = total + four;
                count++;
            }
            var average = ((total) / count);
            average = isNaN(average) ? 0 : average;
            row.vegetationCoverAverageScore(average.toFixed(2));
        }
    });

    var vegetationCoverTableParameters = ['Total canopy %', 'Native canopy %', 'Total understorey %', 'Native understorey %', 'Total ground cover %', 'Native ground cover %', 'No. of structural layers'];
    var totalTransacts = 4;
    var layersRowValues = 0;
    for (var i = 0; i < totalTransacts; i++) {
        var layerCount = self.transients.calculateLayers(i, vegetationCoverTableParameters[0], vegetationCoverTableParameters[1]) +
            self.transients.calculateLayers(i, vegetationCoverTableParameters[2], vegetationCoverTableParameters[3]) +
            self.transients.calculateLayers(i, vegetationCoverTableParameters[4], vegetationCoverTableParameters[5]);
        layersRowValues = +layerCount + layersRowValues;
        $.each(self.data.vegetationCoverTable(), function (j, row) {
            if (row.vegetationCover_parameter() == 'No. of structural layers') {
                if (i == 0) row.vegetationCover_transectOne(layerCount);
                else if (i == 1) row.vegetationCover_transectTwo(layerCount);
                else if (i == 2) row.vegetationCover_transectThree(layerCount);
                else if (i == 3) row.vegetationCover_transectFour(layerCount);
            }
        });
    }

    //If any empty transact column then ignore column for average calculation.
    // Example: activityId: da556bde-abd9-4f38-95e3-686a8a8d9e81
    var vegetationCover_transectOne = 0;
    var vegetationCover_transectTwo = 0;
    var vegetationCover_transectThree = 0;
    var vegetationCover_transectFour = 0;
    var actualTransactCount = 0;

    $.each(self.data.vegetationCoverTable(), function (i, row) {
        if (row.vegetationCover_parameter() != 'No. of structural layers') {
            if(row.vegetationCover_transectOne()){
                vegetationCover_transectOne = 1;
            }
            if(row.vegetationCover_transectTwo()){
                vegetationCover_transectTwo = 1;
            }
            if(row.vegetationCover_transectThree()){
                vegetationCover_transectThree = 1;
            }
            if(row.vegetationCover_transectFour()){
                vegetationCover_transectFour = 1;
            }
        }
    });

    actualTransactCount = vegetationCover_transectOne + vegetationCover_transectTwo + vegetationCover_transectThree + vegetationCover_transectFour;
    $.each(self.data.vegetationCoverTable(), function (i, row) {
        if (row.vegetationCover_parameter() == 'No. of structural layers') {
            var average = Number(layersRowValues) / actualTransactCount;
            row.vegetationCoverAverageScore(average);
        }
    });

};

// Debris
var Output_rarc_EventAndLocationDetails_debrisTableRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent: self});
    self.debris_parameter = ko.observable();

    self.debris_transectOne = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['debris_transectOne'],
            context: context,
            config: config
        }
    });

    self.debris_transectTwo = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['debris_transectTwo'],
            context: context,
            config: config
        }
    });

    self.debris_transectThree = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['debris_transectThree'],
            context: context,
            config: config
        }
    });

    self.debris_transectFour = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['debris_transectFour'],
            context: context,
            config: config
        }
    });

    self.vegetationWidthAverageScore = ko.observable();
    self.loadData = function (data) {
        self['debris_parameter'](ecodata.forms.orDefault(data['debris_parameter'], undefined));
        self['debris_transectOne'](ecodata.forms.orDefault(data['debris_transectOne'], undefined));
        self['debris_transectTwo'](ecodata.forms.orDefault(data['debris_transectTwo'], undefined));
        self['debris_transectThree'](ecodata.forms.orDefault(data['debris_transectThree'], undefined));
        self['debris_transectFour'](ecodata.forms.orDefault(data['debris_transectFour'], undefined));
        self['vegetationWidthAverageScore'](ecodata.forms.orDefault(data['vegetationWidthAverageScore'], 0));
    };
    self.loadData(data || {});

    // Custom script.
    var parameters = ['Total leaf litter', 'Native leaf litter', 'Standing dead trees', 'Hollow bearing trees (>20cm DBH)', 'Fallen logs (>10cm diameter)'];
    self.transients.convertToInt = function (parameter, value) {
        var intValue = '';
        //Leaf litter and native leaf litter cover: 0 = none, 1 = 1–30%, 2 = 31–60%, 3 = 􏰃 60%
        if (parameter == parameters[0] || parameter == parameters[1]) {
            switch (value) {
                case "" : // Nothings selected
                    intValue = -1;
                    break;
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
                    intValue = -1;
                    break;
            }
        }
        //Standing dead trees ( 􏰃 20 cm dbh) and hollow-bearing trees: 0 = absent, 1 = present
        else if (parameter == parameters[2] || parameter == parameters[3]) {
            switch (value) {
                case "" : // Nothings selected
                    intValue = -1;
                    break;
                case "Absent" :
                    intValue = 0;
                    break;
                case "Present":
                    intValue = 1;
                    break;
                default:
                    intValue = -1;
                    break;
            }
        }
        //Fallen logs ( 􏰃 10 cm diameter): 0 = none, 1 = small quantities, 2 = abundant
        else if (parameter == parameters[4]) {
            switch (value) {
                case "" : // Nothings selected
                    intValue = -1;
                    break;
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
                    intValue = -1;
                    break;
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
        if (one >= 0) {
            total = total + one;
            count++;
        }
        if (two >= 0) {
            total = total + two;
            count++;
        }
        if (three >= 0) {
            total = total + three;
            count++;
        }
        if (four >= 0) {
            total = total + four;
            count++;
        }
        var average = (total / count);
        average = isNaN(average) ? 0 : average;
        self.vegetationWidthAverageScore(average.toFixed(2));
        return average;
    }, this);

};

var context = _.extend({}, context, {parent: self, listName: 'debrisTable'});
self.data.debrisTable = ko.observableArray().extend({
    list: {
        metadata: self.dataModel,
        constructorFunction: Output_rarc_EventAndLocationDetails_debrisTableRow,
        context: context,
        userAddedRows: false,
        config: config
    }
});
self.data.debrisTable.loadDefaults = function () {
    self.data.debrisTable.addRow({
        "debris_parameter": "Total leaf litter",
        "totalLeafLitter_T4": "",
        "totalLeafLitter_T3": "",
        "totalLeafLitter_T2": "",
        "totalLeafLitter_T1": "",
        "totalLeafLitter_averageScore": ""
    });
    self.data.debrisTable.addRow({
        "debris_parameter": "Native leaf litter",
        "nativeLeafLitter_T1": "",
        "nativeLeafLitter_T3": "",
        "nativeLeafLitter_T2": "",
        "nativeLeafLitter_T4": "",
        "nativeLeafLitter_averageScore": ""
    });
    self.data.debrisTable.addRow({
        "deadTrees_averageScore": "",
        "debris_parameter": "Standing dead trees",
        "deadTrees_T3": "",
        "deadTrees_T2": "",
        "deadTrees_T4": "",
        "deadTrees_T1": ""
    });
    self.data.debrisTable.addRow({
        "debris_parameter": "Hollow bearing trees (>20cm DBH)",
        "hollowBearingTrees_T1": "",
        "hollowBearingTrees_averageScore": "",
        "hollowBearingTrees_T2": "",
        "hollowBearingTrees_T3": "",
        "hollowBearingTrees_T4": ""
    });
    self.data.debrisTable.addRow({
        "debris_parameter": "Fallen logs (>10cm diameter)",
        "logs_averageScoreTotal": "",
        "logs_T3_score": "",
        "logs_T1_score": "",
        "logs_T4_score": "",
        "noEdit": true,
        "logs_T2_score": ""
    });
};

// FeaturesTable
var Output_rarc_EventAndLocationDetails_featuresTableRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent: self});
    self.feature_parameter = ko.observable();

    self.feature_transectOne = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['feature_transectOne'],
            context: context,
            config: config
        }
    });

    self.feature_transectTwo = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['feature_transectTwo'],
            context: context,
            config: config
        }
    });

    self.feature_transectThree = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['feature_transectThree'],
            context: context,
            config: config
        }
    });

    self.feature_transectFour = ko.observable().extend({
        metadata: {
            metadata: self.dataModel['feature_transectFour'],
            context: context,
            config: config
        }
    });

    self.featuresAverageScore = ko.observable();
    self.loadData = function (data) {
        self['feature_parameter'](ecodata.forms.orDefault(data['feature_parameter'], undefined));
        self['feature_transectOne'](ecodata.forms.orDefault(data['feature_transectOne'], undefined));
        self['feature_transectTwo'](ecodata.forms.orDefault(data['feature_transectTwo'], undefined));
        self['feature_transectThree'](ecodata.forms.orDefault(data['feature_transectThree'], undefined));
        self['feature_transectFour'](ecodata.forms.orDefault(data['feature_transectFour'], undefined));
        self['featuresAverageScore'](ecodata.forms.orDefault(data['featuresAverageScore'], undefined));
    };
    self.loadData(data || {});

    // Custom
    self.transients.convertToInt = function (parameter, value) {
        var intValue = '';
        if (parameter == 'Reeds' || parameter == 'Large native tussock grasses') {
            switch (value) {
                case "" : // Nothings selected
                    intValue = -1;
                    break;
                case "None" :
                    intValue = 0;
                    break;
                case "Scattered":
                    intValue = 1;
                    break;
                case "Abundant":
                    intValue = 2;
                    break;
                default:
                    intValue = -1;
                    break;
            }
        } else {
            switch (value) {
                case "" : // Nothings selected
                    intValue = -1;
                    break;
                case "None" :
                    intValue = 0;
                    break;
                case "Scattered":
                    intValue = 1;
                    break;
                case "Abundant":
                    intValue = 2;
                    break;
                case "Abundant with grazing damage":
                    intValue = 1.5;
                default:
                    intValue = -1;
                    break;
            }
        }
        return intValue;
    };

    self.transients.featuresAverageScore = ko.computed(function () {
        var one, two, three, four, count = 0, total = 0;
        one = self.transients.convertToInt(self.feature_parameter(), self.feature_transectOne());
        two = self.transients.convertToInt(self.feature_parameter(), self.feature_transectTwo());
        three = self.transients.convertToInt(self.feature_parameter(), self.feature_transectThree());
        four = self.transients.convertToInt(self.feature_parameter(), self.feature_transectFour());
        if (one >= 0) {
            total = total + one;
            count++;
        }
        if (two >= 0) {
            total = total + two;
            count++;
        }
        if (three >= 0) {
            total = total + three;
            count++;
        }
        if (four >= 0) {
            total = total + four;
            count++;
        }
        var average = ((total) / count);
        average = isNaN(average) ? 0 : average;
        self.featuresAverageScore(average.toFixed(2));
        return average;
    }, this);
};

var context = _.extend({}, context, {parent: self, listName: 'featuresTable'});
self.data.featuresTable = ko.observableArray().extend({
    list: {
        metadata: self.dataModel,
        constructorFunction: Output_rarc_EventAndLocationDetails_featuresTableRow,
        context: context,
        userAddedRows: false,
        config: config
    }
});
self.data.featuresTable.loadDefaults = function () {
    self.data.featuresTable.addRow({
        "nativeCanopyRegen_T3": "",
        "nativeCanopyRegen_T4": "",
        "nativeCanopyRegen_averageScore": "",
        "feature_parameter": "Native canopy species regeneration",
        "nativeCanopyRegen_T1": "",
        "nativeCanopyRegen_T2": ""
    });
    self.data.featuresTable.addRow({
        "nativeUnderstoreyRegen_averageScore": "",
        "feature_parameter": "Native understorey regeneration",
        "nativeUnderstoreyRegen_T1": "",
        "nativeUnderstoreyRegen_T2": "",
        "nativeUnderstoreyRegen_T3": "",
        "nativeUnderstoreyRegen_T4": ""
    });
    self.data.featuresTable.addRow({
        "tussockGrasses_averageScore": "",
        "feature_parameter": "Large native tussock grasses",
        "tussockGrasses_T4": "",
        "tussockGrasses_T3": "",
        "tussockGrasses_T2": "",
        "tussockGrasses_T1": ""
    });
    self.data.featuresTable.addRow({
        "reeds_T1": "",
        "reeds_T4": "",
        "feature_parameter": "Reeds",
        "reeds_T3": "",
        "reeds_T2": "",
        "reeds_averageScore": ""
    });
};

//---- Overall calculation
self.data.proximityScore = ko.observable();
self.data.continuityScore = ko.observable();
self.data.habitatScoreTotal = ko.observable();
self.data.coverScoreTotal = ko.observable();
self.data.nativesScoreTotal = ko.observable();
self.data.debrisScoreTotal = ko.observable();
self.data.featuresScoreTotal = ko.observable();
self.data.totalScoreAggregate = ko.observable();
self.data.overallQuality = ko.observable();
self.transients.proximityScore = ko.computed(function () {
    var v = self.data.nearestNativeVegetationGreaterThan10ha();
    var proximityScore = 0;
    switch (v) {
        case "200 m - 1 km":
            proximityScore = 1;
            break;
        case "Contiguous":
            proximityScore = 2;
            break;
        case "Contiguous with patch > 50 ha":
            proximityScore = 3;
            break;
        default:
            proximityScore = 0;
    }
    self.data.proximityScore(proximityScore);
    return proximityScore;
}, this);

self.transients.continuityScore = ko.computed(function () {
    var v = self.data.proportionOfRiverBankWithVegetationGreaterThan5mDeep();
    var d = self.data.numberOfDiscontinuitiesGreaterThan50m() * 0.5;
    var continuityScore = 0;
    switch (v) {
        case "50 - 64%":
            continuityScore = d >= 1 ? 0 : (1 - d).toFixed(2);
            break;
        case "65 - 79%":
            continuityScore = d >= 2 ? 0 : (2 - d).toFixed(2);
            break;
        case "80 - 94%":
            continuityScore = d >= 3 ? 0 : (3 - d).toFixed(2);
            break;
        case "> 95%":
            continuityScore = d >= 4 ? 0 : (4 - d).toFixed(2);
            break;
        default:
            continuityScore = 0;
    }
    self.data.continuityScore(continuityScore);
    return continuityScore;
}, this);
self.transients.habitatScoreTotal = ko.computed(function () {
    var a = self.data.continuityScore() ? self.data.continuityScore() : 0;
    var b = self.transients.tempCanopyVegetationTableScore() ? self.transients.tempCanopyVegetationTableScore() : 0;
    var c = self.data.proximityScore() ? self.data.proximityScore() : 0;
    var total = Number(a) + Number(b) + Number(c);
    self.data.habitatScoreTotal(total);
});
self.transients.featuresScoreTotal = ko.computed(function () {
    var total = 0;
    $.each(self.data.featuresTable(), function (i, object) {
        total = total + Number(object.featuresAverageScore());
    });
    self.data.featuresScoreTotal(total);
});

self.transients.debrisScoreTotal = ko.computed(function () {
    var total = 0;
    $.each(self.data.debrisTable(), function (i, object) {
        total = total + Number(object.vegetationWidthAverageScore());
    });
    self.data.debrisScoreTotal(total);
});

self.transients.coverScoreTotal = ko.computed(function () {
    var total = 0;
    var nativesScoreTotal = 0;
    $.each(self.data.vegetationCoverTable(), function (i, object) {
        var parameter = object.vegetationCover_parameter();
        if (parameter == 'Total canopy %' || parameter == 'Total understorey %' || parameter == 'Total ground cover %' || parameter == 'No. of structural layers')
            total = total + Number(object.vegetationCoverAverageScore());
        else if (parameter == 'Native canopy %' || parameter == 'Native understorey %' || parameter == 'Native ground cover %') {
            nativesScoreTotal = nativesScoreTotal + Number(object.vegetationCoverAverageScore());
        }
    });
    self.data.coverScoreTotal(total);
    self.data.nativesScoreTotal(nativesScoreTotal);
});

self.transients.totalScoreAggregate = ko.computed(function () {
    var total = 0;
    var d = Number(self.data.debrisScoreTotal()) ? Number(self.data.debrisScoreTotal()) : 0;
    var f = Number(self.data.featuresScoreTotal()) ? Number(self.data.featuresScoreTotal()) : 0;
    var h = Number(self.data.habitatScoreTotal()) ? Number(self.data.habitatScoreTotal()) : 0;
    var c = Number(self.data.coverScoreTotal()) ? Number(self.data.coverScoreTotal()) : 0;
    var n = Number(self.data.nativesScoreTotal()) ? Number(self.data.nativesScoreTotal()) : 0;
    total = d + f + h + c + n;
    self.data.totalScoreAggregate(total);
});

self.transients.overallQuality = ko.computed(function () {
    self.transients.updateVegetationCoverTableLayers();
    var v = Number(self.data.totalScoreAggregate());
    var quality = '';
    if (v <= 10) quality = "Degraded";
    else if (v <= 20) quality = "Poor";
    else if (v <= 30) quality = "Fair";
    else if (v <= 40) quality = "Good";
    else quality = "Excellent";
    self.data.overallQuality(quality);
});