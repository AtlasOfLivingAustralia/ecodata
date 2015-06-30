var projects = db.project.find({'custom.details.objectives.rows1.assets':'World Heriatge area'});

while (projects.hasNext()) {
    var project = projects.next();

    for (var i=0; i<project.custom.details.objectives.rows1.length; i++) {
        for (var j=0; j<project.custom.details.objectives.rows1[i].assets.length; j++) {
            if (project.custom.details.objectives.rows1[i].assets[j] == 'World Heriatge area') {
                project.custom.details.objectives.rows1[i].assets[j] = 'World Heritage area';

            }
        }

    }
    print("Updating project "+project.projectId);
    db.project.save(project);

}


projects = db.project.find({'custom.details.objectives.rows1.assets':'Remnat Vegetation'});

while (projects.hasNext()) {
    var project = projects.next();

    for (var i=0; i<project.custom.details.objectives.rows1.length; i++) {
        for (var j=0; j<project.custom.details.objectives.rows1[i].assets.length; j++) {
            if (project.custom.details.objectives.rows1[i].assets[j] == 'Remnat Vegetation') {
                project.custom.details.objectives.rows1[i].assets[j] = 'Remnant Vegetation';

            }
        }

    }
    print("Updating project "+project.projectId);
    db.project.save(project);

}


projects = db.project.find({'custom.details.objectives.rows1.assets':'Community awareness/particpation in NRM'});

while (projects.hasNext()) {
    var project = projects.next();

    for (var i=0; i<project.custom.details.objectives.rows1.length; i++) {
        for (var j=0; j<project.custom.details.objectives.rows1[i].assets.length; j++) {
            if (project.custom.details.objectives.rows1[i].assets[j] == 'Community awareness/particpation in NRM') {
                project.custom.details.objectives.rows1[i].assets[j] = 'Community awareness/participation in NRM';

            }
        }

    }
    print("Updating project "+project.projectId);
    db.project.save(project);

}