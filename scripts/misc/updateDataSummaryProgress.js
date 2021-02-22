
// This change is done under https://github.com/AtlasOfLivingAustralia/fieldcapture/issues/2170

print("Start to update dataset summary progress");

db.project.find({isMERIT:true, "custom.dataSets" : {$ne:null}, "custom.dataSets.progress":null})
    .forEach(function (project) {
        project.custom.dataSets.forEach(function (dataset) {
            if (dataset.progress == null) {
                dataset.progress="started";
            }
        });
        db.project.save(project);
    });

print("Completed!");