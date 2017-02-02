var projects = db.project.find({status:{$ne:'deleted'}, isMERIT:true});
var label = 'No. of plants planted by mature height class (eg. >2m)';

function findOutputTargetIndex(outputTargets, label) {
    for (var i=0; i<outputTargets.length; i++) {
        var target = outputTargets[i];
        if (target.scoreLabel == label) {
            return i;
        }
    }
    return -1;
}

while (projects.hasNext()) {
    var project = projects.next();

    if (project.outputTargets) {
        var i = findOutputTargetIndex(project.outputTargets, label);
        if (i >=0) {

            project.outputTargets.splice(i,1);
            db.project.save(project);

        }
    }
}