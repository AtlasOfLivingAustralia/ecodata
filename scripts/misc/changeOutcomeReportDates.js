var programs = db.program.find({parent:{$exists:true}});
while (programs.hasNext()) {
    var program = programs.next();

    if (program.config && program.config.projectReports) {
        for (var i=0; i<program.config.projectReports.length; i++) {
            if (program.config.projectReports[i].activityType == 'RLP Short term project outcomes') {
                program.config.projectReports[i].firstReportingPeriodEnd = "2021-06-30T14:00:00Z";

            }
            else if (program.config.projectReports[i].activityType == 'RLP Medium term project outcomes') {
                program.config.projectReports[i].firstReportingPeriodEnd = "2023-06-30T14:00:00Z";
            }
        }
        db.program.save(program);
    }
    else {
        print("***************************Error: no config for program "+program.programId);
    }
}