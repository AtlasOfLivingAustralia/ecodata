var mus = db.managementUnit.find({status:{$ne:'deleted'}});
while (mus.hasNext())  {
    var mu = mus.next();
    var modified = false;
    if (mu.config && mu.config.projectReports) {
        for (var i=0; i<mu.config.projectReports.length; i++)  {
            if (mu.config.projectReports[i].adjustmentActivityType) {
                delete mu.config.projectReports[i].adjustmentActivityType;
                modified = true;
            }
        }
        if (modified == true){
            print("Removing adjustments for: "+mu.name);
            db.managementUnit.save(mu);
        }
    }
}