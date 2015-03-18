

function renameOutputTargetScore(oldName, newName) {
    print("Renaming "+oldName+" to "+newName);

    var projects = db.project.find({'outputTargets.scoreLabel':oldName});
    print("Found "+projects.count()+" projects");
    var totalUpdated = 0;
    while (projects.hasNext()) {

        var perProjectCount = 0;
        var project = projects.next();

        for (var i=0; i<project.outputTargets.length; i++) {
            if (project.outputTargets[i].scoreLabel === oldName) {
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


renameOutputTargetScore('Total No. of unique participants attending project events', 'Total No. of new participants (attending project events for the first time)');