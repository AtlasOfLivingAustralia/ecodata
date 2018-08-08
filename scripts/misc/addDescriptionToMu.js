var mus = db.program.find({parent:{$exists:true}});
while (mus.hasNext()) {
    var mu = mus.next();

    if (mu.config.projectReports) {
        for (var i=0; i<mu.config.projectReports.length; i++) {
            if (mu.config.projectReports[i].category == 'Outputs Reporting') {
                mu.config.projectReports[i].description = '_Output report templates are still being finalised for RLP services. These templates will be completed in August 2018_';

                print("Update MU: "+mu.name);
                db.program.save(mu);
            }
        }

    }


}
