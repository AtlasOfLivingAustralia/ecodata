var outputs = db.output.find({name:'Management Practice Change Details', 'data.targetOutcomes':{$exists:true}});
var outputCount = outputs.count();
var count = 0;


while (outputs.hasNext()) {
    var output = outputs.next();

    print("Updating Management Practice Details for activity: "+output.activityId+", output: "+output.outputId);
    var targetOutcomes = output.data.targetOutcomes;
    var practiceChange = output.data.practiceChange;

    for (var i=0; i<practiceChange.length; i++) {
        practiceChange[i].targetOutcomes = [];
        for (var j=0; j<targetOutcomes.length; j++) {
            if (targetOutcomes[j] == 'Reduced transport of nutrients off-farm' ||
                targetOutcomes[j] == 'Reduced transport of pesticides off-farm' ||
                targetOutcomes[j] == 'Reduced transport of farm chemicals off-farm') {
                practiceChange[i].targetOutcomes.push('Improve water quality by reducing the transport of nutrients, pesticides and other farm chemicals off-farm');
            }
            else if (targetOutcomes[j] == 'Improved soil structure, health and fertility / productivity') {
                practiceChange[i].targetOutcomes.push('Building soil carbon to improve nutrients and soil water holding capacity');
            }
            else if (targetOutcomes[j] == 'Reduced transport of soils / sediment off-farm') {
                practiceChange[i].targetOutcomes.push('Improve air and water quality by reducing the transport of soils / sediment off-farm (wind and water erosion)');
            }
            else if (targetOutcomes[j] == 'Enhanced habitat for native flora and fauna') {
                practiceChange[i].targetOutcomes.push('Increased adoption of industry approved environment management systems');
            }
            else {
                practiceChange[i].targetOutcomes.push(targetOutcomes[j]);
            }
        }
        if (practiceChange[i].practiceChangeAction == 'Retaining ground cover' ||
            practiceChange[i].practiceChangeAction == 'Minimum-to-no till farming' ||
            practiceChange[i].practiceChangeAction == 'Composting/mulching' ||
            practiceChange[i].practiceChangeAction == 'Planting cover crops' ||
            practiceChange[i].practiceChangeAction == 'Soil conditioning' ||
            practiceChange[i].practiceChangeAction == 'Managing soil nutrients' ||
            practiceChange[i].practiceChangeAction == 'By-catch reduction' ||
            practiceChange[i].practiceChangeAction == 'Industry codes of practice' ||
            practiceChange[i].practiceChangeAction == 'Reduced stocking rates/selective grazing' ||
            practiceChange[i].practiceChangeAction == 'Consistent wheel tracks' ||
            practiceChange[i].practiceChangeAction == 'Crop rotation improvements' ||
            practiceChange[i].practiceChangeAction == 'Nitrogen budget developed') {

            practiceChange[i].practiceChangeAction = ['Development and implementation of industry codes of practice or guidelines'];
        }
        else if (practiceChange[i].practiceChangeAction == 'Soil testing'){
            practiceChange[i].practiceChangeAction = ['Application and testing of on-farm decision support tools'];
        }
        else if (practiceChange[i].practiceChangeAction == 'Farm planning' ||
                 practiceChange[i].practiceChangeAction == 'Planning' ||
                 practiceChange[i].practiceChangeAction == 'Industry environmental management plan') {
            practiceChange[i].practiceChangeAction = ['Farm planning and extension'];
        }
        else if (practiceChange[i].practiceChangeAction == 'Establishing wind breaks' ||
                practiceChange[i].practiceChangeAction == 'Fence off stock to watering points' ||
                practiceChange[i].practiceChangeAction == 'Revegetation' ||
                practiceChange[i].practiceChangeAction == 'Restoration' ||
                practiceChange[i].practiceChangeAction == 'Riparian fencing' ||
                practiceChange[i].practiceChangeAction == 'Management of invasive species') {
            practiceChange[i].practiceChangeAction = ['On-ground works (specify rationale and private contributions in notes)'];
        }
        else if (practiceChange[i].practiceChangeAction == 'Chemical application considered with rain forecast' ||
                 practiceChange[i].practiceChangeAction == 'Improved technologies in management') {
            practiceChange[i].practiceChangeAction = ['Use of new technologies to monitor and manage stocking rates'];
        }
        else if (practiceChange[i].practiceChangeAction == 'Other (specify in notes)'){
            practiceChange[i].practiceChangeAction = ['Other (specify in notes)'];
        }
        else {
            throw {name:'Error', description:'Missing option: '+practiceChange[i].practiceChangeAction};
        }

        if (practiceChange[i].changePurpose == 'Innovative farming and fishing practices') {
            practiceChange[i].changePurpose = ['Sustainable fishing and aquaculture practice'];
        }
        else {
            practiceChange[i].changePurpose = [practiceChange[i].changePurpose];
        }
    }

    delete output.data.targetOutcomes;

    db.output.save(output);
    count++;
}
if (outputCount != count) {
    print("Error! Expected "+outputCount+" but modified "+count+" Management Practice Change Details outputs");
}
else {
    print("Updated "+count+" Management Practice Change Details outputs");
}