var orgs = [];
var projects = db.project.find({status:{$ne:'deleted'}, isCitizenScience:true});
while (projects.hasNext()) {
    var project = projects.next();
    if (project.name != 'Individual Sightings' && project.name != "Peggy's Test Project" && project.name != "ALA's 'BioCollect' software - test project" && project.name != "ALA species sightings and OzAtlas") {

        printOrg(project.organisationId);
        if (project.associatedOrgs) {
            for (var i=0; i<project.associatedOrgs.length; i++) {
                printOrg(project.associatedOrgs[i].organisationId);
            }
        }
    }
}


function printOrg(organisationId) {
    if (orgs.indexOf(organisationId) < 0) {
        orgs.push(organisationId);
        print('./exportOrganisation.sh '+organisationId);
    }

}