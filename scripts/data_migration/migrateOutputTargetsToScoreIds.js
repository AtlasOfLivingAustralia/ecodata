var projects = db.project.find({status:{$ne:'deleted'}, isWorks:true});

function findTarget(outputTargets, label) {
    for (var i=0; i<outputTargets.length; i++) {
        var target = outputTargets[i];
        if (target.scoreLabel == label) {
            return target;
        }
    }
    return false;
}

function hasTarget(target) {
   return target.target && target.target != "0";
}

function removeTarget(outputTargets, label) {

}

function checkPair(outputTargets, label1, label2) {
    var t1 = findTarget(outputTargets, label1);
    if (t1) {
        var sub = findTarget(outputTargets, label2);
        if (!t1.target) {
            printjson(t1);
        }
        print("1. "+label1 +"="+t1.target+", 2. "+label2+"="+(sub?sub.target:'n/a'));
    }
}


while (projects.hasNext()) {
    var project = projects.next();

    if (project.outputTargets) {

        checkPair(project.outputTargets, 'Total new area treated (Ha)', 'Total new area treated for weeds (Ha)');
        checkPair(project.outputTargets, 'No. of site assessments undertaken', 'No. of site assessments undertaken using the Commonwealth government vegetation assessment methodology');
        checkPair(project.outputTargets, 'Farming entities adopting sustainable practice change', 'Total No. of farming entities adopting sustainable practice change');
        checkPair(project.outputTargets, 'Burnt area (Ha) - may be different to the total area managed', 'Burnt area (Ha)');
        checkPair(project.outputTargets, 'Area managed with stock (Ha)', 'Area managed with conservation grazing (Ha)');

        checkPair(project.outputTargets, 'No. of farming entities adopting sustainable practice change', 'Total No. of farming entities adopting sustainable practice change');

        for (var i=0; i<project.outputTargets.length; i++) {

            var target = project.outputTargets[i];

            if (target.scoreLabel == 'No of Indigenous participants at project events. ') {
                target.scoreLabel = 'No of Indigenous participants at project events.';
            }
            if (target.scoreLabel == 'Total No. of plants grown and ready for planting ') {
                target.scoreLabel = 'Total No. of plants grown and ready for planting';
            }
            if (target.scoreLabel == 'Total No. of Community Participation and Engagement events run') {
                target.scoreLabel = 'Total No. of community participation and engagement events run'
            }


            if (target.scoreLabel) {

                var score = db.score.find({label:target.scoreLabel});
                if (score.hasNext()) {
                    target.scoreId = score.next().scoreId;
                    db.project.save(project);
                }
                else {
                    print("No score matching: "+target.scoreLabel+" for project "+project.projectId);

                }
            }
        }
    }

}
