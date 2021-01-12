//jsMain: https://dl.dropbox.com/s/qc2cyjqfkx04da5/siteBcScore.js?dl=0
//Update Bootbox to latest version
var jQueryScript = document.createElement('script');
jQueryScript.setAttribute('src', 'https://cdnjs.cloudflare.com/ajax/libs/bootbox.js/5.3.2/bootbox.min.js');
document.head.appendChild(jQueryScript);

self.supportedBioregions = {};
self.bioConditionAssessmentTableReference = {};
self.bioConditionBenchmarks = {};
self.selectedEcosystemBenchmark = {};

self.data.benchmarkMarkGroundCoverNativeGrassCover = ko.observable(); // Not in the form
self.data.benchmarkMarkGroundCoverOrganicLitterCover = ko.observable(); // Not in the form


self.initialiseReferenceTable = function () {
    if ($.isEmptyObject(self.bioConditionAssessmentTableReference)) {
        self.bioConditionAssessmentTableReference = self.loadLookups('BioConditionAssessmentTableReference');
    }
};

self.initialiseBenchmarkTable = function () {
    if ($.isEmptyObject(self.bioConditionBenchmarks)) {
        self.bioConditionBenchmarks = self.loadLookups('BioConditionAssessmentBenchmarks');
    }
};


self.updateScore = function(){
    // Calculate
   /* var avg = 0;
    var total = (parseFloat(self.data.largeTreesScore()) +
        parseFloat(self.data.aveCanopyHeightScore()) +
        parseFloat(self.data.emergentHeightScore()) +
        parseFloat(self.data.edlCanopyHeightScore()) +
        parseFloat(self.data.edlRecruitmentScore()) +
        parseFloat(self.data.siteBcScore()) +
        parseFloat(self.data.numTreeSpeciesTotal()) +
        parseFloat(self.data.numShrubSpeciesTotal()) +
        parseFloat(self.data.numGrassSpeciesTotal()) +
        parseFloat(self.data.numForbSpeciesTotal));

    // TODO Fix this
    self.data.siteBcScore(total > 0 ? parseFloat(total)/10 : 0);
    */
};


// Example keys: Recruitment of dominant canopy species (%)
self.lookupBenchmarkValues = function(key) {
    var value = '';
    if (!$.isEmptyObject(self.selectedEcosystemBenchmark)) {
        $.grep(self.selectedEcosystemBenchmark ? self.selectedEcosystemBenchmark : [], function (row) {
            if (row.name == key) {
                value = row.value;
            }
        });
    }
    return value;
};

self.loadLookups = function (key) {
    // Do ajax webservice call to Biocondition reference table and benchmark values
    var result = {};
    var bioConditionAssessmentBioRegionsUrl = 'https://dl.dropbox.com/s/78eit7ozq4in64j/SupportedBioRegions.json?dl=0';
    var bioConditionAssessmentTableReferenceUrl = 'https://dl.dropbox.com/s/6i46fd8yzz2yf5r/BioConditionAssessmentTableReference.json?dl=0';
    var bioConditionAssessmentBenchmarksUrl = 'https://dl.dropbox.com/s/z0n8e2ua92f3ktd/Benchmark-BRB-BrigalowBelt.json?dl=0';

    var url = '';
    switch (key) {
        case 'BioConditionAssessmentBioRegions':
            url = bioConditionAssessmentBioRegionsUrl;
            break;
        case 'BioConditionAssessmentTableReference':
            url = bioConditionAssessmentTableReferenceUrl;
            break;
        case 'BioConditionAssessmentBenchmarks':
            url = bioConditionAssessmentBenchmarksUrl;
            break;

        default:
            break;
    }

    $.ajax({
        url: url,
        dataType: 'json',
        async: false,
        success: function (data) {
            result = data
        },
        beforeSend: function(){
            blockUIWithMessage("<p class='text-center'>Loading...</p>");
        },
        complete: function () {
            $.unblockUI();
        }
    });

    return result;
};

self.data.siteBcScore = ko.observable().extend({numericString: 2});

// Populate Benchmark value.
self.data.bioregion.subscribe(function (obj) {
    var table;
    if ($.isEmptyObject(self.supportedBioregions)) {
        self.supportedBioregions = self.loadLookups('BioConditionAssessmentBioRegions');
    }
    $.grep(!$.isEmptyObject(self.supportedBioregions) ? self.supportedBioregions.value : [], function (row) {
        if (row.key == 'regions') {
            table = row;
        }
    });

    $.grep(table ? table.value : [], function (row) {
        if (row.name == self.data.bioregion()) {
            ecosystemCode = row.value;
        }
    });

    var codes = ecosystemCode ? ecosystemCode.split(',') : [];
    var ecoSystemsArray = [];
    ecoSystemsArray.push({text: 'Select ecosystems', value: ''});
    $.each(codes, function (index, value) {
        value = value ? value.trim() : '';
        ecoSystemsArray.push({text: value, value: value});
    });

    //TODO
    var pathname = window.location.pathname;
    if(pathname.includes("create")){
        bootbox.prompt({
            title: "<h1>Select regional ecosystem / landscape<h1>",
            inputType: 'select',
            inputOptions: ecoSystemsArray,
            callback: function (result) {
                if(result === null) {
                    // Cancel button pressed.
                } else {
                    self.setBenchmarkValues(result);
                }
            }
        });
    }

});

self.setBenchmarkValues = function(benchmarkCode){
    // TODO
    // self.data.regionalEcosystem(benchmarkCode);

    // Testing Birgalow ecosystem
    self.data.regionalEcosystem("11.4.12");
    $.grep(!$.isEmptyObject(self.bioConditionBenchmarks) ? self.bioConditionBenchmarks.value : [], function (row) {
        if (row.key == '11.4.12') {
            self.selectedEcosystemBenchmark = row.value;  // Store current benchmark table value
        }
    });

    //Column 1
    self.data.benchmarkEucalyptLargeTreeNo(self.lookupBenchmarkValues('Trees - Large trees - Number of large eucalypt trees per hectare'));
    self.data.benchmarkEucalyptLargeTreeDBH(self.lookupBenchmarkValues('Trees - Large trees - Large eucalypt tree dbh threshold (cm)'));
    self.data.benchmarkNonEucalyptLargeTreeNo(self.lookupBenchmarkValues('Trees - Large trees - Number of large non-eucalypt trees per hectare'));
    self.data.benchmarkNonEucalyptLargeTreeDBH(self.lookupBenchmarkValues('Trees - Large trees - Large non-eucalypt tree dbh threshold (cm)'));
    self.data.benchmarkCWD(self.lookupBenchmarkValues('Coarse woody debris - Total length (m) of debris ≥ 10cm diameter and ≥0.5m in length per hectare'));

    //Column 2
    self.data.benchmarkTreeEDLHeight(self.lookupBenchmarkValues('Trees - Emergent canopy - Tree emergent canopy median height (m)'));
    self.data.benchmarkTreeCanopyHeight(self.lookupBenchmarkValues('Trees - Tree canopy - Tree canopy median height (m)'));
    self.data.benchmarkSubCanopyHeight(self.lookupBenchmarkValues('Trees - Tree sub-canopy - Tree sub-canopy median height (m)'));
    self.data.benchmarkEdlSpeciesRecruitment(self.lookupBenchmarkValues('Recruitment of dominant canopy species (%)'));

    //Column 3
    self.data.benchmarkNumTreeSpeciesTotal(self.lookupBenchmarkValues('Native plant species richness - Tree'));
    self.data.benchmarkNumShrubSpeciesTotal(self.lookupBenchmarkValues('Native plant species richness - Shrub'));
    self.data.benchmarkNumGrassSpeciesTotal(self.lookupBenchmarkValues('Native plant species richness - Grass'));
    self.data.benchmarkNumForbSpeciesTotal(self.lookupBenchmarkValues('Native plant species richness - Forbes and Other'));
    self.data.benchmarkSpeciesCoverExotic(self.lookupBenchmarkValues('Non-native plant cover - Typical non-native species'));

    // Custom
    self.data.benchmarkMarkGroundCoverNativeGrassCover = self.lookupBenchmarkValues('Ground cover (%) - Native perennial grass cover (%)');
    self.data.benchmarkMarkGroundCoverOrganicLitterCover = self.lookupBenchmarkValues('Ground cover (%) - Organic litter cover (%)');

};

// >> START of 100 x 50m Area - Ecologically Dominant Layer
// A. 5.1.1 Large Trees - Table 5.
self.transients.largeTreesScore  =  ko.computed(function () {
    var numLargeEucalypt = parseInt(self.data.numLargeEucalypt());
    var numLargeNonEucalypt = parseInt(self.data.numLargeNonEucalypt());

    self.data.totalLargeTrees(numLargeEucalypt + numLargeNonEucalypt);
    self.data.numLargeEucalyptPerHa(numLargeEucalypt * 2);
    self.data.numLargeNonEucalyptPerHa(numLargeNonEucalypt * 2);
    self.data.totalLargeTreesPerHa(parseInt(self.data.numLargeEucalyptPerHa()) + parseInt(self.data.numLargeNonEucalyptPerHa()));


    var benchmarkNumLargeEucalypt = self.data.benchmarkEucalyptLargeTreeNo();
    var benchmarkNumLargeNonEucalypt = self.data.benchmarkNonEucalyptLargeTreeNo();
    var assessmentPercentage = 0;
    if(benchmarkTotalLargeTrees != 'na' && !isNaN(benchmarkNumLargeEucalypt) && !isNaN(benchmarkNumLargeNonEucalypt) && benchmarkTotalLargeTrees > 0){
        var benchmarkTotalLargeTrees = parseInt(benchmarkNumLargeEucalypt) + parseInt(benchmarkNumLargeNonEucalypt);
        assessmentPercentage = (parseInt(self.data.totalLargeTreesPerHa())/parseInt(benchmarkTotalLargeTrees)) * 100;
    }

    // Get the table value
    var table;
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_5') {
            table = row;
        }
    });

    // Calculate the score.
    assessmentPercentage = parseInt(assessmentPercentage);
    var score = 0;
    if (table && table.value && table.value.length == 4) {
        if (table.value[0].name == 'No large trees present' && assessmentPercentage <= 0) {
            score = table.value[0].value;
        } else if (table.value[1].name == '0 to 50% of benchmark number of large trees' && assessmentPercentage > 0 && assessmentPercentage < 50) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥50% to 100% of benchmark number of large trees' && assessmentPercentage >= 50 && assessmentPercentage < 100) {
            score = table.value[2].value;
        } else if (table.value[3].name == '≥ benchmark number of large trees' && assessmentPercentage >= 100 ) {
            score = table.value[3].value;
        }
    }

    self.data.largeTreesScore(score);
});

// B. 5.1.2 Tree canapy height - Table 6
self.transients.aveCanopyHeightScore = function () {
    var count = 0;
    parseInt(self.data.subcanopyHeightScore()) > 0 ? count++ : 0;
    parseInt(self.data.edlCanopyHeightScore()) > 0 ? count++ : 0;
    parseInt(self.data.emergentHeightScore()) > 0 ? count++ : 0;
    var avg = (parseInt(self.data.subcanopyHeightScore()) + parseInt(self.data.edlCanopyHeightScore()) + parseInt(self.data.emergentHeightScore()))/parseInt(count);
    avg = avg > 0 ? avg : 0;
    self.data.aveCanopyHeightScore(avg);
};


self.transients.emergentHeightInMetres = ko.computed(function () {
 // 1. Get the emergentHeight value.
 // 2. Get the benchmark value.
 // 3. emergentHeight value is what % of benchmark value.
 // 4. if <25% | >=25% to 70% | >=70%
    var emergentHeight = self.data.emergentHeightInMetres();
    var benchmarkEmergentHeight = self.data.benchmarkTreeEDLHeight();
    var assessmentPercentage = 0;
    if(benchmarkEmergentHeight != 'na' && !isNaN(benchmarkEmergentHeight) && !isNaN(emergentHeight) && benchmarkEmergentHeight > 0) {
        assessmentPercentage = parseInt(emergentHeight)/parseInt(benchmarkEmergentHeight) * 100;
    }

    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_6') {
            table = row;
        }
    });
    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark height' && assessmentPercentage <= 0) {
            score = table.value[0].value;
        }
        else if (table.value[1].name == '≥25% to 70% of benchmark height' && assessmentPercentage >= 25 && assessmentPercentage < 70) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥70% of benchmark height' && assessmentPercentage >= 70) {
            score = table.value[2].value;
        }
    }

    self.data.emergentHeightScore(score);
    self.transients.aveCanopyHeightScore();

    self.updateScore();
});

self.transients.edlCanopyHeightScore = ko.computed(function () {
    // 1. Get the emergentHeight value.
    // 2. Get the benchmark value.
    // 3. emergentHeight value is what % of benchmark value.
    // 4. if <25% | >=25% to 70% | >=70%
    var treeCanopyHeight = self.data.treeCanopyHeightInMetres();
    var benchmarkTreeCanopyHeight = self.data.benchmarkTreeCanopyHeight();
    var assessmentPercentage = 0;
    if(benchmarkTreeCanopyHeight != 'na' && !isNaN(benchmarkTreeCanopyHeight) && !isNaN(treeCanopyHeight) && benchmarkTreeCanopyHeight > 0) {
        assessmentPercentage = parseInt(treeCanopyHeight)/parseInt(benchmarkTreeCanopyHeight) * 100;
    }

    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_6') {
            table = row;
        }
    });
    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark height' && assessmentPercentage <= 0) {
            score = table.value[0].value;
        }
        else if (table.value[1].name == '≥25% to 70% of benchmark height' && assessmentPercentage >= 25 && assessmentPercentage < 70) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥70% of benchmark height' && assessmentPercentage >= 70) {
            score = table.value[2].value;
        }
    }

    self.data.edlCanopyHeightScore(score);
    self.transients.aveCanopyHeightScore();
    self.updateScore();
});

self.transients.subcanopyHeightScore = ko.computed(function () {
    var subcanopyHeight = self.data.subcanopyHeightInMetres();
    var benchmarkSubCanopyHeight = self.data.benchmarkSubCanopyHeight();
    var assessmentPercentage = 0;
    if(benchmarkSubCanopyHeight != 'na' && !isNaN(benchmarkSubCanopyHeight) && !isNaN(subcanopyHeight) && benchmarkSubCanopyHeight >0) {
        assessmentPercentage = parseInt(subcanopyHeight)/parseInt(benchmarkSubCanopyHeight) * 100;
    }

    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_6') {
            table = row;
        }
    });
    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark height' && assessmentPercentage <= 0) {
            score = table.value[0].value;
        }
        else if (table.value[1].name == '≥25% to 70% of benchmark height' && assessmentPercentage >= 25 && assessmentPercentage < 70) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥70% of benchmark height' && assessmentPercentage >= 70) {
            score = table.value[2].value;
        }
    }

    self.data.subcanopyHeightScore(score);
    self.transients.aveCanopyHeightScore();
    self.updateScore();
});


// C. 5.1.3 Recruitment of dominant canopy species TODO: Check whether we need to use benchmark value
self.transients.edlRecruitmentScore  = ko.computed(function () {
    var proportionDominantCanopySpeciesWithEvidenceOfRecruitment = self.data.proportionDominantCanopySpeciesWithEvidenceOfRecruitment();
    var assessmentPercentage = proportionDominantCanopySpeciesWithEvidenceOfRecruitment;

    /*var benchmarkEdlSpeciesRecruitment = self.data.benchmarkEdlSpeciesRecruitment();
    if(benchmarkEdlSpeciesRecruitment != 'na' && !isNaN(benchmarkEdlSpeciesRecruitment) && !isNaN(proportionDominantCanopySpeciesWithEvidenceOfRecruitment) && benchmarkEdlSpeciesRecruitment > 0) {
        assessmentPercentage = parseInt(parseInt(proportionDominantCanopySpeciesWithEvidenceOfRecruitment)/parseInt(benchmarkEdlSpeciesRecruitment)) * 100;
    }*/

    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_7') {
            table = row;
        }
    });
    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<20% of dominant canopy* species present as regeneration' && assessmentPercentage < 20) {
            score = table.value[0].value;
        }
        else if (table.value[1].name == '≥20 – 75% of dominant canopy* species present as regeneration' && assessmentPercentage >= 20 && assessmentPercentage < 75) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥75% of dominant canopy* species present as regeneration' && assessmentPercentage >= 75) {
            score = table.value[2].value;
        }
    }

    self.data.edlRecruitmentScore(score);
    self.updateScore();
});

// 5.1.4 Native tree species richness - Page 18 - Table 11

//>>
var Output_BioConditionConsolidated_treeSpeciesRichnessRow = function (data, dataModel, context, config) {
    var self = this;
    ecodata.forms.NestedModel.apply(self, [data, dataModel, context, config]);
    context = _.extend(context, {parent:self});            var speciesNameTreeConfig = _.extend(config, {printable:'', dataFieldName:'speciesNameTree', output: 'BioCondition Method', surveyName: '' });
    self.speciesNameTree = new SpeciesViewModel({}, speciesNameTreeConfig);
    self.loadData = function(data) {
        self['speciesNameTree'].loadData(ecodata.forms.orDefault(data['speciesNameTree'], {}));
    };
    self.loadData(data || {});
};
var context = _.extend({}, context, {parent:self, listName:'treeSpeciesRichness'});
self.data.treeSpeciesRichness = ko.observableArray().extend({list:{metadata:self.dataModel, constructorFunction:Output_BioConditionConsolidated_treeSpeciesRichnessRow, context:context, userAddedRows:true, config:config}});
self.data.treeSpeciesRichness.loadDefaults = function() {
};
//<<

self.data.treeSpeciesRichness.subscribe(function (obj) {
    self.data.numTreeSpecies(self.data.treeSpeciesRichness().length);

    var numTreeSpecies = self.data.numTreeSpecies();
    var benchmarkNumTreeSpeciesTotal = self.data.benchmarkNumTreeSpeciesTotal();
    var assessmentPercentage = 0;
    if(benchmarkNumTreeSpeciesTotal != 'na' && !isNaN(numTreeSpecies) && !isNaN(benchmarkNumTreeSpeciesTotal) && benchmarkNumTreeSpeciesTotal > 0) {
        assessmentPercentage = parseInt(numTreeSpecies)/parseInt(benchmarkNumTreeSpeciesTotal) * 100;
    }

    var score = 0;
    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_11') {
            table = row;
        }
    });

    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark number of species within each life-form' && assessmentPercentage < 25) {
            score = table.value[0].value;
        } else if (table.value[1].name == '≥25% to 90% of benchmark number of species within each life-form' && assessmentPercentage >= 25 && assessmentPercentage < 90) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥90% of benchmark number of species within each life-form' && assessmentPercentage >= 90) {
            score = table.value[2].value;
        }
    }

    // TODO: Check whether we need to calculate number of unknown species.
    self.data.numTreeSpeciesTotal(score);

    self.updateScore();
});
// << END OF 100 x 50m Area

// 5.4 50x10mplot
// 5.4.1 Native plant species richness - Table 11
self.data.shrubSpeciesRichness.subscribe(function (obj) {
    self.data.numShrubSpecies(self.data.shrubSpeciesRichness().length);

    var numShrubSpecies = self.data.numShrubSpecies();
    var benchmarkNumShrubSpeciesTotal = self.data.benchmarkNumShrubSpeciesTotal();
    var assessmentPercentage = 0;
    if(benchmarkNumShrubSpeciesTotal != 'na' && !isNaN(numShrubSpecies) && !isNaN(benchmarkNumShrubSpeciesTotal) && benchmarkNumShrubSpeciesTotal > 0) {
        assessmentPercentage = parseInt(numShrubSpecies)/parseInt(benchmarkNumShrubSpeciesTotal) * 100;
    }

    var score = 0;
    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_11') {
            table = row;
        }
    });

    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark number of species within each life-form' && assessmentPercentage < 25) {
            score = table.value[0].value;
        } else if (table.value[1].name == '≥25% to 90% of benchmark number of species within each life-form' && assessmentPercentage >= 25 && assessmentPercentage < 90) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥90% of benchmark number of species within each life-form' && assessmentPercentage >= 90) {
            score = table.value[2].value;
        }
    }

    // TODO: Check whether we need to calculate number of unknown species.
    self.data.numShrubSpeciesTotal(score);
    self.updateScore();
});

self.data.grassSpeciesRichness.subscribe(function (obj) {
    self.data.numGrassSpecies(self.data.grassSpeciesRichness().length);

    var numGrassSpecies = self.data.numGrassSpecies();
    var benchmarkNumGrassSpeciesTotal = self.data.benchmarkNumGrassSpeciesTotal();
    var assessmentPercentage = 0;
    if(benchmarkNumGrassSpeciesTotal != 'na' && !isNaN(numGrassSpecies) && !isNaN(benchmarkNumGrassSpeciesTotal) && benchmarkNumGrassSpeciesTotal > 0) {
        assessmentPercentage = parseInt(numGrassSpecies)/parseInt(benchmarkNumGrassSpeciesTotal) * 100;
    }

    var score = 0;
    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_11') {
            table = row;
        }
    });

    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark number of species within each life-form' && assessmentPercentage < 25) {
            score = table.value[0].value;
        } else if (table.value[1].name == '≥25% to 90% of benchmark number of species within each life-form' && assessmentPercentage >= 25 && assessmentPercentage < 90) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥90% of benchmark number of species within each life-form' && assessmentPercentage >= 90) {
            score = table.value[2].value;
        }
    }

    // TODO: Check whether we need to calculate number of unknown species.
    self.data.numGrassSpeciesTotal(score);
    self.updateScore();
});


self.data.forbsAndOtherNonGrassGroundSpeciesRichness.subscribe(function (obj) {
    self.data.numForbSpecies(self.data.forbsAndOtherNonGrassGroundSpeciesRichness().length);

    var numForbSpecies = self.data.numForbSpecies();
    var benchmarkNumForbSpeciesTotal = self.data.benchmarkNumForbSpeciesTotal();
    var assessmentPercentage = 0;
    if(benchmarkNumForbSpeciesTotal != 'na' && !isNaN(numForbSpecies) && !isNaN(benchmarkNumForbSpeciesTotal) && benchmarkNumForbSpeciesTotal > 0) {
        assessmentPercentage = parseInt(numForbSpecies)/parseInt(benchmarkNumForbSpeciesTotal) * 100;
    }

    var score = 0;
    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_11') {
            table = row;
        }
    });

    var score = 0;
    if(table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<25% of benchmark number of species within each life-form' && assessmentPercentage < 25) {
            score = table.value[0].value;
        } else if (table.value[1].name == '≥25% to 90% of benchmark number of species within each life-form' && assessmentPercentage >= 25 && assessmentPercentage < 90) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥90% of benchmark number of species within each life-form' && assessmentPercentage >= 90) {
            score = table.value[2].value;
        }
    }
    // TODO: Check whether we need to calculate number of unknown species.
    self.data.numForbSpeciesTotal(score);
    self.updateScore();
});

// 5.4.2 Non-native plant cover
self.data.nonNativeSpeciesRichness.subscribe(function (obj) {
    self.data.numNonNativeSpecies(self.data.nonNativeSpeciesRichness().length);

    var numNonNativeSpecies = self.data.numNonNativeSpecies();
    var benchmarkSpeciesCoverExotic = self.data.benchmarkSpeciesCoverExotic();
    var assessmentPercentage = 0;
    if(benchmarkSpeciesCoverExotic != 'na' && !isNaN(numNonNativeSpecies) && !isNaN(benchmarkSpeciesCoverExotic) && benchmarkSpeciesCoverExotic > 0) {
        assessmentPercentage = parseInt(parseInt(numNonNativeSpecies)/parseInt(benchmarkSpeciesCoverExotic)) * 100;
    }

    var score = 0;
    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_12') {
            table = row;
        }
    });

    var score = 0;
    if(table && table.value && table.value.length == 4) {
        if (table.value[0].name == '>50% of vegetation cover are non-native plants' && assessmentPercentage > 50) {
            score = table.value[0].value;
        } else if (table.value[1].name == '≥25 – 50% of vegetation cover are non-native plants' && assessmentPercentage >= 25 && assessmentPercentage < 50) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥5 – 25% of vegetation cover are non-native plants' && assessmentPercentage >= 5 && assessmentPercentage < 25) {
            score = table.value[2].value;
        } else if (table.value[3].name == '≥5 – 25% of vegetation cover are non-native plants' && assessmentPercentage < 5) {
            score = table.value[3].value;
        }
    }

    self.data.nonNativePlantCoverPercent(score);
    self.updateScore();
    // TODO: Check whether we need to calculate number of unknown species.
    // TODO: self.data.numNonNativeSpeciesTotal(score); - Benchmark not available.
});


// Calculate 50 x 20m area - Coarse Woody Debris. - 5.3.1 Coarsewoodydebris
self.data.totalCwdLength.subscribe(function (obj){
    var benchmarkValue = self.lookupBenchmarkValues('Coarse woody debris - Total length (m) of debris ≥ 10cm diameter and ≥0.5m in length per hectare');
    var totalCwdLength = obj;
    if(isNaN(totalCwdLength) || totalCwdLength == 0 || isNaN(benchmarkValue)){
        return;
    }

    var table;
    // Get the Table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_10') {
            table = row;
        }
    });

    var bmv10 = (benchmarkValue * 10) / 100;
    var bmv50 = (benchmarkValue * 50) / 100;
    var bmv200 = (benchmarkValue * 200) / 100;

    if (table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<10% of benchmark number or total length of CWD' && totalCwdLength  < bmv10 ) {
            self.data.cwdScore(table.value[0].value);
        } else if (table.value[1].name == '>/= 10 to <50% or >200% of benchmark number or total length of CWD' &&
            (totalCwdLength >= bmv10 && totalCwdLength < bmv50) ||
            (totalCwdLength > bmv200)) {
            self.data.cwdScore(table.value[1].value);
        } else if (table.value[2].name == '≥50% or ≤200% of benchmark number or total length of CWD' &&
            (totalCwdLength >= bmv50 || totalCwdLength <= bmv200)) {
            self.data.cwdScore(table.value[2].value);
        }
    }
});

// Five 1x1m plots - Ground Cover
// Refer groundCover.js
// << End of Five 1x1m plots - Ground Cover

// Calculate - 100m Transect
// 5.2.1 Tree Canopy Cover
self.data.treeCanopyRecords.subscribe(function (obj){
    var percentCoverC = 0;
    var percentCoverS = 0;
    var percentCoverE = 0;
    var cCount = 0;
    var sCount = 0;
    var eCount = 0;
    // TODO : Does type of tree matter ie; exotic and native?
    $.each(self.data.treeCanopyRecords(), function( index, value ) {
        if(value.treeOrTreeGroup() == 'C') {
            percentCoverC = parseFloat(percentCoverC) + parseFloat(value.distance()) - parseFloat(value.totalTCCover());
            cCount++;
        } else if(value.treeOrTreeGroup() == 'S') {
            percentCoverS = parseFloat(percentCoverS) + parseFloat(value.distance()) - parseFloat(value.totalTCCover());
            sCount++;
        } else if (value.treeOrTreeGroup() == 'E') {
            percentCoverE = parseFloat(percentCoverE) + parseFloat(value.distance()) - parseFloat(value.totalTCCover());
            eCount++;
        }
    });

    self.data.percentCoverC(percentCoverC);
    self.data.percentCoverS(percentCoverS);
    self.data.percentCoverE(percentCoverE);

    var benchmarkTreeCanapyCover = self.lookupBenchmarkValues('Trees - Tree canopy - Tree canopy cover (%)');
    var benchmarkTreeSubCanapyCover = self.lookupBenchmarkValues('Trees - Tree sub-canopy - Tree sub-canopy cover (%)');
    var benchmarkTreeEmergentCover = self.lookupBenchmarkValues('Trees - Emergent canopy - Tree emergent canopy cover (%)');

    var assessmentPercentage = 0;
    if(benchmarkTreeCanapyCover != 'na' && !isNaN(percentCoverC) && !isNaN(benchmarkTreeCanapyCover) && benchmarkTreeCanapyCover > 0) {
        assessmentPercentage = parseInt(parseInt(percentCoverC)/parseInt(benchmarkTreeCanapyCover)) * 100;
    }
    self.data.coverScoreC(self.genericTableLookupScore('table_8',assessmentPercentage));

    assessmentPercentage = 0;
    if(benchmarkTreeSubCanapyCover != 'na' && !isNaN(percentCoverS) && !isNaN(benchmarkTreeSubCanapyCover) && benchmarkTreeSubCanapyCover > 0) {
        assessmentPercentage = parseInt(parseInt(percentCoverS)/parseInt(benchmarkTreeSubCanapyCover)) * 100;
    }
    self.data.coverScoreS(self.genericTableLookupScore('table_8',assessmentPercentage));

    assessmentPercentage = 0;
    if(benchmarkTreeEmergentCover != 'na' && !isNaN(percentCoverE) && !isNaN(benchmarkTreeEmergentCover) && benchmarkTreeEmergentCover > 0) {
        assessmentPercentage = parseInt(parseInt(percentCoverE)/parseInt(benchmarkTreeEmergentCover)) * 100;
    }
    self.data.coverScoreE(self.genericTableLookupScore('table_8',assessmentPercentage));

    var treeCanopyCoverScoreAve = (cCount+sCount+eCount)/3; // TODO divided by 2 or 3? Should we include 0 entry?
    self.data.treeCanopyCoverScoreAve(treeCanopyCoverScoreAve);
    self.updateScore();
});

// 5.2.2 Shrub Cover
self.data.shrubCanopyRecords.subscribe(function (obj){
    var percentCoverNative = 0;
    var percentCoverExotic = 0;

    // TODO : Does type of tree matter ie; exotic and native?
    $.each(self.data.shrubCanopyRecords(), function( index, value ) {
        if(value.shrubType() == 'native') {
            percentCoverNative = parseFloat(percentCoverNative) + parseFloat(value.distance()) - parseFloat(value.totalSCCover());
        } else if(value.shrubType() == 'exotic') {
            percentCoverExotic = parseFloat(percentCoverExotic) + parseFloat(value.distance()) - parseFloat(value.totalSCCover());
        }
    });

    self.data.percentCoverNative(percentCoverNative);
    self.data.percentCoverExotic(percentCoverExotic);

    var benchmarkShrubCoverNative = self.lookupBenchmarkValues('Shrubs - Native shrub cover (%)');
    var assessmentPercentage = 0;
    if(benchmarkShrubCoverNative != 'na' && !isNaN(percentCoverNative) && !isNaN(benchmarkShrubCoverNative) && benchmarkShrubCoverNative > 0) {
        assessmentPercentage = parseInt(parseInt(percentCoverNative)/parseInt(benchmarkShrubCoverNative)) * 100;
    }

    self.data.shrubCanopyCoverScoreN(self.genericTableLookupScore('table_9',assessmentPercentage));
    self.updateScore();
});

self.genericTableLookupScore = function(tableName, assessmentPercentage) {
    var score = 0;
    var table;
    // Get the table value
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == tableName) {
            table = row;
        }
    });


    if(tableName == 'table_8' && table && table.value && table.value.length == 4) {
        if (table.value[0].name == '<10' && assessmentPercentage < 10) {
            score = table.value[0].value;
        } else if (table.value[1].name == '>=10% and <50%' && assessmentPercentage >= 10 && assessmentPercentage < 50) {
            score = table.value[1].value;
        } else if (table.value[2].name == '>=50% or <=200%' && assessmentPercentage >= 50 && assessmentPercentage <= 200) {
            score = table.value[2].value;
        } else if (table.value[3].name == '>200%' && assessmentPercentage > 200) {
            score = table.value[3].value;
        }
    }

    if(tableName == 'table_9' && table && table.value && table.value.length == 3) {
        if (table.value[0].name == '<10% of benchmark shrub cover' && assessmentPercentage < 10) {
            score = table.value[0].value;
        } else if (table.value[1].name == '>/= 10 to <50% or >200% of benchmark shrub cover' && ((assessmentPercentage >= 10 && assessmentPercentage < 50) || assessmentPercentage >= 200)) {
            score = table.value[1].value;
        } else if (table.value[2].name == '≥50% or ≤200% of benchmark shrub cover' && assessmentPercentage >= 50 && assessmentPercentage <= 200) {
            score = table.value[2].value;
        }
    }

    return score;
};




// Calculate - Assessment - Landscape Attributes
// Start of 6.1 Landscaping
self.data.patchSize.subscribe(function (obj) {
    var table;
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_15') {
            table = row;
        }
    });

    $.grep(table ? table.value : [], function (row) {
        if (row.name == self.data.patchSize()) {
            self.data.patchSizeScore(row.value);
        }
    });
});
self.data.connectivity.subscribe(function (obj) {
    var table;
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_16') {
            table = row;
        }
    });

    $.grep(table ? table.value : [], function (row) {
        if (row.name == self.data.connectivity()) {
            self.data.connectivityScore(row.value);
        }
    });

    self.updateScore();
});

self.data.landscapeContext.subscribe(function (obj) {
    var table;

    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_17') {
            table = row;
        }
    });

    $.grep(table ? table.value : [], function (row) {
        if (row.name == self.data.landscapeContext()) {
            self.data.landscapeContextScore(row.value);
        }
    });

    self.updateScore();
});

self.data.distanceFromWater.subscribe(function (obj) {
    var table;
    $.grep(!$.isEmptyObject(self.bioConditionAssessmentTableReference) ? self.bioConditionAssessmentTableReference.value : [], function (row) {
        if (row.key == 'table_18') {
            table = row;
        }
    });

    $.grep(table ? table.value : [], function (row) {
        if (row.name == self.data.distanceFromWater()) {
            self.data.distanceFromWaterScore(row.value);
        }
    });
    self.updateScore();
});
// End of 6.1 Landscaping

self.initialiseReferenceTable();
self.initialiseBenchmarkTable();
