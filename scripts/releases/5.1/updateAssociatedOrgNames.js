load('../../utils/audit.js');
let projects = db.project.find({status:{$ne:'deleted'}, associatedOrgs:{$exists:true}, isMERIT:false});
while (projects.hasNext()) {
    let changed = false;

    let project = projects.next();
    let associatedOrgs = project.associatedOrgs;
    if (associatedOrgs) {
        for (let i = 0; i < associatedOrgs.length; i++) {
            if (associatedOrgs[i].organisationId) {
                let org = db.organisation.findOne({organisationId: associatedOrgs[i].organisationId});
                if (org) {
                    if (org.name != associatedOrgs[i].name) {
                        print("Updating associated org for project " + project.projectId + " from " + associatedOrgs[i].name + " to " + org.name);
                        associatedOrgs[i].name = org.name;
                        changed = true;
                    }
                } else {
                    print("No organisation found for associated org " + associatedOrgs[i].organisationId + " in project " + project.projectId);
                }

            }
        }
        if (changed) {
            db.project.replaceOne({projectId: project.projectId}, project);
            audit(project, project.projectId, 'au.org.ala.ecodata.Project', 'system');
        }

    }
}