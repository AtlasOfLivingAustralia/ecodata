var projects = db.project.find({associatedProgram:'Environmental Stewardship', status:'active', name:/Environmental Stewardship Program project on property/});
var siteCount = 0;
while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects: project.projectId});
    if (!sites.hasNext()) {
        print("Warning: no sites found for project "+project.projectId+" grant id="+project.grantId+" external id="+project.externalId);
    }

    var projectArea = false;
    while (sites.hasNext()) {
        var site = sites.next();
        if (site.type == 'projectArea') {
            projectArea = true;
        }
    }
    if (!projectArea) {
        print("Warning:  no project area found for project "+project.projectId+" grant id="+project.grantId+" external id="+project.externalId);
    }

    if (sites.count() > 0) {
        siteCount = siteCount+sites.count();
        if (projectArea) {
            siteCount = siteCount -1;
        }
    }
}
print("Total site count: "+siteCount);

