var projects = db.project.find({'custom.details.lastUpdated':{$gte:'2014-12-02T08:00:00Z'}});

var affectedProjects = {};
while (projects.hasNext()) {
    var project = projects.next();

    var auditTrail = db.auditMessage.find({
        projectId: project.projectId,
        entityType: 'au.org.ala.ecodata.Project',
        'entity.custom.details.budget': {$exists: true}
    }).sort({'entity.custom.details.lastUpdated': -1});


    var currentTotal = 0;
    while (auditTrail.hasNext()) {
        var entry = auditTrail.next();

        var total = Number(entry.entity.custom.details.budget.overallTotal);
        print(project.projectId+','+entry.entity.custom.details.lastUpdated+', '+total);

        if (currentTotal == 0) {
            if (total > 0) {
                if (affectedProjects[project.projectId]) {
                    affectedProjects[project.projectId]['fixed'] = true;
                }
            }

        }
        else {
            if (total == 0) {
                affectedProjects[project.projectId] = {affected: true, org:project.organisationName};
            }

        }
        currentTotal = total;
    }

}
printjson(affectedProjects);
