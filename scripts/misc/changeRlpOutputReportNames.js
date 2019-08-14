var programs = db.program.find({parent:{$exists:true}});
while (programs.hasNext()) {
    var program = programs.next();

    if (program.config && program.config.projectReports) {
        for (var i=0; i<program.config.projectReports.length; i++) {
            if (program.config.projectReports[i].activityType == 'RLP Output Report') {
                program.config.projectReports[i].reportNameFormat = "%5$s - %6$s Outputs Report %7$02d";
                program.config.projectReports[i].reportDescriptionFormat = "%5$s - %6$s Outputs Report %7$02d";
            }
        }
        db.program.save(program);
    }
    else {
        print("***************************Error: no config for program "+program.programId);
    }
}