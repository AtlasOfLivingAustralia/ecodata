

function renameOutputTargetScore(outputLabel, oldName, newName) {
    print("Renaming "+oldName+" to "+newName);

    var projects = db.project.find({'outputTargets.outputLabel':outputLabel, 'outputTargets.scoreLabel':oldName});
    print("Found "+projects.count()+" projects");
    var totalUpdated = 0;
    while (projects.hasNext()) {

        var perProjectCount = 0;
        var project = projects.next();

        for (var i=0; i<project.outputTargets.length; i++) {
            if (project.outputTargets[i].scoreLabel === oldName && project.outputTargets[i].outputLabel === outputLabel) {
                project.outputTargets[i].scoreLabel = newName;
                perProjectCount++;
            }
        }

        print("Updating project "+project.projectId+" renamed "+perProjectCount+" scores");
        db.project.save(project);
        totalUpdated++;
    }
    print("Renamed scores for "+totalUpdated+" projects");

}

//renameOutputTargetScore('Weed Treatment Details', 'Total new area treated (Ha)', 'Total new area treated for weeds (Ha)');
//renameOutputTargetScore('Upload of stage 1 and 2 reporting data', 'Total new area treated (Ha)', 'Total new area treated for weeds (Ha)');
//renameOutputTargetScore('Output Details', 'Total new area treated (Ha)', 'Total new area treated for weeds (Ha)');
//renameOutputTargetScore('Output Details', 'Total new area of weeds treated (Ha)', 'Total new area treated for weeds (Ha)');


//renameOutputTargetScore('Fauna Survey Details', 'No. of surveys undertaken', 'No. of fauna surveys undertaken');
//renameOutputTargetScore('Flora Survey Details', 'No. of surveys undertaken', 'No. of flora surveys undertaken');

//renameOutputTargetScore('Weed Treatment Details', 'Total area treated (Ha)', 'Total new area treated (Ha)');
//renameOutputTargetScore('Pest Management Details', 'No. of individual animals killed / removed by species', 'Total No. of individuals or colonies of pest animals destroyed');

//renameOutputTargetScore('Revegetation Details', 'Kilograms of seed sown of species expected to grow  > 2 metres in height', 'Kilograms of seed sown of species expected to grow < 2 metres in height');

//renameOutputTargetScore('Management Practice Change Details', 'Farming entities adopting sustainable practice change', 'Total No. of farming entities adopting sustainable practice change');
//renameOutputTargetScore('Fire Management Details', 'Burnt area (Ha) - may be different to the total area managed', 'Burnt area (Ha)');

renameOutputTargetScore('Participant Information', 'Total No of participants attending project activities', 'No of volunteers participating in project activities');

