var toChange = 'Soil erosion';
var newValue = 'Soil carbon';
var affectedProjects = db.project.find({'custom.details.outcomes.primaryOutcome.assets':toChange});
if (affectedProjects.count()) {
    throw "Uhoh";
}
var secondaryProjects = db.project.find({'custom.details.outcomes.secondaryOutcomes.assets':toChange});
if (secondaryProjects.count()) {
    throw "Gah";
}

var programs = db.program.find({});
while (programs.hasNext()) {
    var program = programs.next();

    var found = false;
    for (var i=0; i<(program.priorities || []).length; i++) {
        if (program.priorities[i].priority == toChange) {
            program.priorities[i].priority = newValue;
            found = true;
        }

    }
    if (found) {
        print("Updating program: "+program.name);
        db.program.save(program);

    }


}