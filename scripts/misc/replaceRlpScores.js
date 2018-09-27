function replaceOutputTargetMeasure(oldLabel, newLabel) {
    var oldScore = db.score.find({label:oldLabel});
    var newScore = db.score.find({label:newLabel});
    if (oldScore.count() != 1 || newScore.count() != 1) {
        throw "Error updating target: "+oldLabel+"count = , "+oldScore.count()+", "+newLabel+", count= "+newScore.count();
    }

    var oldScoreId = oldScore.next().scoreId;
    var newScoreId = newScore.next().scoreId;

    var affectedProjects = db.project.find({'outputTargets.scoreId':oldScoreId});
    while (affectedProjects.hasNext()) {
        var project = affectedProjects.next();

        var found = false;
        for (var i=0; i<project.outputTargets.length; i++) {
            if (project.outputTargets[i].scoreId == oldScoreId) {
                print("Replacing score: "+oldLabel+" with "+newLabel+" for project: "+project.projectId+", name: "+project.name);
                project.outputTargets[i].scoreId = newScoreId;
                found = true;
            }
        }
        if (!found) {
            throw("Should have found score but didn't for project: "+project.projectId);
        }
        db.project.save(project);
    }

}


replaceOutputTargetMeasure('Area (ha) treated for weeds', 'Area (ha) treated for weeds - initial');
db.score.remove({category:'RLP', label:'Area (ha) treated for weeds'});

replaceOutputTargetMeasure('Area (ha) treated for pest animals', 'Area (ha) treated for pest animals - initial');
db.score.remove({category:'RLP', label:'Area (ha) treated for pest animals'});

replaceOutputTargetMeasure('Number of permanent agreements', 'Number of agreements');
db.score.remove({category:'RLP', label:'Number of permanent agreements'});