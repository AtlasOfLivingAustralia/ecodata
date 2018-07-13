// Example Activity URL: https://biocollect.ala.org.au/actwaterwatch/bioActivity/create/b3efa2eb-ed40-4dc8-ae0a-0c9f0c25ab0e
// Activity Name: Rapid Appraisal of Riparian Condition (RARC)
// Output model name: rarc_EventAndLocationDetails
// jsMain: https://dl.dropbox.com/s/fmyel0nh83tl87h/canopyVegetationTable.js?dl=0
// minifiled: https://dl.dropbox.com/s/ghpu4yaahpo27iu/canopyVegetationTable.min.js?dl=0
self.data.canopyVegetationTable = ko.observableArray([]);
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
    var cwRow = ko.utils.arrayFirst(self.data.canopyVegetationTable(), function(row) {
        return row.parameter() == "Channel Width (CW)";
    });
    var vwRow = ko.utils.arrayFirst(self.data.canopyVegetationTable(), function(row) {
        return row.parameter() == "Vegetation Width (VW)";
    });
    $.each(self.data.canopyVegetationTable(), function (i, canopy) {
        if(canopy.parameter() == 'Score') {
            canopy.transectOne(self.transients.canopyVegetationTableScore(cwRow.transectOne(), vwRow.transectOne()));
            canopy.transectTwo(self.transients.canopyVegetationTableScore(cwRow.transectTwo(), vwRow.transectTwo()));
            canopy.transectThree(self.transients.canopyVegetationTableScore(cwRow.transectThree(), vwRow.transectThree()));
            canopy.transectFour(self.transients.canopyVegetationTableScore(cwRow.transectFour(), vwRow.transectFour()));
            var averageScore = ((canopy.transectOne() + canopy.transectTwo() + canopy.transectThree() + canopy.transectFour())/4).toFixed(2);
            canopy.vegetationWidthAverageScore(averageScore);
            self.transients.tempCanopyVegetationTableScore(averageScore)
        } else {
            canopy.vegetationWidthAverageScore("");
            self.transients.tempCanopyVegetationTableScore(0)
        }
    });

};
self.selectedcanopyVegetationTableRow = ko.observable();
self.data.canopyVegetationTable.subscribe(function(obj) {
    self.transients.updateCanopyVegetationScore();
});
self.transients.canopyVegetationTableDirtyItems = ko.computed(function() {
    self.transients.updateCanopyVegetationScore();
}, this);

//Default behaviour.
self.loadcanopyVegetationTable = function (data, append) {
    if (!append) {
        self.data.canopyVegetationTable([]);
    }
    if (data === undefined) {
        self.data.canopyVegetationTable.push(new CanopyVegetationTableRow({"parameter":"Channel Width (CW)","channelWidth_averageScore":"","channelWidth_T3":"","channelWidth_T2":"","channelWidth_T1":"","channelWidth_T4":""}));
        self.data.canopyVegetationTable.push(new CanopyVegetationTableRow({"parameter":"Vegetation Width (VW)","vegetationWidth_averageScore":"","vegetationWidth_T3":"","vegetationWidth_T4":"","vegetationWidth_T1":"","vegetationWidth_T2":""}));
        self.data.canopyVegetationTable.push(new CanopyVegetationTableRow({"vegetationWidth_T1_score":"","vegetationWidth_T2_score":"","vegetationWidth_averageScoreTotal":"","parameter":"Score","vegetationWidth_T3_score":"","vegetationWidth_T4_score":""}));
    } else {
        $.each(data, function (i, obj) {
            self.data.canopyVegetationTable.push(new CanopyVegetationTableRow(obj));
        });
    }
};
self.addcanopyVegetationTableRow = function () {
    var newRow = new CanopyVegetationTableRow();
    self.data.canopyVegetationTable.push(newRow);
};
self.removecanopyVegetationTableRow = function (row) {
    blockUIWithMessage("<p class='text-center'>Not Permitted</p>");
    setTimeout(function(){
        $.unblockUI();
    }, 1500);
};
self.canopyVegetationTablerowCount = function () {
    return self.data.canopyVegetationTable().length;
};
