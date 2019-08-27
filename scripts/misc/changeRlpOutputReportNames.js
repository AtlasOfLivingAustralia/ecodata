var programs = db.program.find({parent:{$exists:true}});
while (programs.hasNext()) {
    var program = programs.next();

    if (program.config && program.config.projectReports) {
        for (var i=0; i<program.config.projectReports.length; i++) {
            if (program.config.projectReports[i].activityType == 'RLP Output Report') {
                program.config.projectReports[i].reportNameFormat = "Year %5\$s - %6\$s %7\$d Outputs Report";
                program.config.projectReports[i].reportDescriptionFormat = "Year %5\$s - %6\$s %7\$d Outputs Report";
            }
        }
        db.program.save(program);
    }
    else {
        print("***************************Error: no config for program "+program.programId);
    }
}