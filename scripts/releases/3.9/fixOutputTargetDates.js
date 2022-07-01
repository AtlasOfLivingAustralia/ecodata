var projects = db.project.find();
while (projects.hasNext()) {
    var project = projects.next();
    var changed = false;
    if (project.outputTargets) {
        if (!project.isMERIT) {
            print("Found biocollect project with output targets! "+project.projectId);
        }
        for (var i=0; i<project.outputTargets.length; i++) {
            if (project.outputTargets[i].targetDate == "" || project.outputTargets[i].targetDate === undefined) {
                project.outputTargets[i].targetDate = null;
                changed = true;
            }
            else if (project.outputTargets[i].targetDate != null && typeof project.outputTargets[i].targetDate === 'string') {
                project.outputTargets[i].targetDate = ISODate(project.outputTargets[i].targetDate);
                changed = true;
            }
        }
    }
    if (changed) {
        db.project.save(project);
    }
}
