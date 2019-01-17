var projects = db.project.find({associatedProgram:'Environmental Stewardship', associatedSubProgram:{$ne:""}, status:'active'});

print(projects.count());
while (projects.hasNext()) {
    var project = projects.next()
    print(project.name);
    var sites = db.site.find({projects:project.projectId});

    while (sites.hasNext()) {
        var site = sites.next();
        if (site.type != 'projectArea' && site.extent.geometry) {

            if (site.extent.source == 'pid' && site.extent.geometry != 'pid' && site.extent.geometry.coordinates) {
                print(site.name);

                print(site.extent.source);
                print(site.extent.geometry.type);

                site.extent.source = 'drawn';
                db.site.save(site);

            }
        }

    }
}
