var projects = db.project.find({status:{$ne:'deleted'}, isCitizenScience:true});
while (projects.hasNext()) {
    var project = projects.next();
    if (project.name != 'Individual Sightings' && project.name != "Peggy's Test Project" && project.name != "ALA's 'BioCollect' software - test project" && project.name != "ALA species sightings and OzAtlas") {
        print('./exportProject.sh ' + project.projectId);
    }
}