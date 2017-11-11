var projects = db.project.find({associatedProgram:'Environmental Stewardship', status:'active'});
while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects:project.projectId});
    if (sites.count() == 0) {
        print("No sites for project: "+project.grantId);
    }
}