load('renameListValue.js');

// Revegetation Details
renameListValue('Revegetation Details','revegetationMethod', 'Hand broardcast seeding', 'Hand broadcast seeding');
renameListValue('Revegetation Details', 'connectivityIndex', 'Isolated forest or woodland remnant', 'Isolated forest or woodland remnant >1km from other remnants');
renameListValue('Revegetation Details', 'connectivityIndex', 'Isolated grassland', 'Isolated grassland >1km from other remnants');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of more than 1000 ha', 'Patch <1km from a patch of more than 1000ha');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of 100 to 1000 ha', 'Patch <1km from a patch of 100 to 1000ha');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of 25 to 100 ha', 'Patch <1km from a patch of 25 to 100ha');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of less than 25 ha', 'Patch <1km from a patch of less than 25ha');

renameArrayTypedListValue('Revegetation Details', 'environmentalBenefits', 'Habitat restoration - home range improvement', 'Home range / extent improvement');
renameArrayTypedListValue('Revegetation Details', 'environmentalBenefits', 'Habitat enhancement - improved migration paths', 'Improved habitat connectivity');
renameArrayTypedListValue('Revegetation Details', 'environmentalBenefits', 'Streambank protection', 'Riparian rehabilitation');

var outputs = db.output.find({name:'Revegetation Details', 'data.guardType':{$exists:false}});
var outputCount = outputs.count();
var count = 0;

while (outputs.hasNext()) {
    var output = outputs.next();

    output.data.guardType = [];

    print("Updating Revegetation Details for activity: " + output.activityId + ", output: " + output.outputId);

    var planting = output.data.planting;

    for (var i=0; i<planting.length; i++) {

        var guardType = planting[i].guardType;
        if (guardType) {
            if (guardType == 'Gro-guard') {
                guardType = 'Plastic sleeve';
            }
            if (output.data.guardType.indexOf(guardType) < 0) {
                output.data.guardType.push(guardType);
            }
        }
        delete planting[i].guardType;
    }

    db.output.save(output);
    count++;
}

if (outputCount != count) {
    print("Error! Expected "+outputCount+" but modified "+count+" Revegetation Details outputs");
}
else {
    print("Updated "+count+" Revegetation Details outputs");
}