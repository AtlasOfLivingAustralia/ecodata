var managementUnits = db.managementUnit.find({status:{$ne:'deleted'}});
while (managementUnits.hasNext()) {
    var managementUnit = managementUnits.next();

    if (managementUnit.config && managementUnit.config.projectReports) {
        for (var i=0; i<managementUnit.config.projectReports.length; i++) {
            if (managementUnit.config.projectReports[i].activityType == 'RLP Annual Report') {
                managementUnit.config.projectReports[i].reportNameFormat = "Year %5$s - Annual Report";
                managementUnit.config.projectReports[i].reportDescriptionFormat = "Year %5$s - Annual Report";
            }
        }
        db.managementUnit.save(managementUnit);
    }
    else {
        print("***************************Error: no config for program "+managementUnit.managementUnitId);
    }
}