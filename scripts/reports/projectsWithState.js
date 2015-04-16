
var projects = db.project.find({status:{$ne:'deleted'}});

print('Name, Grant ID, External ID, Organisation Name, Programme, Sub-Programme, Planned Start Date, Planned End Date, Funding, Status, Description, State, State, State');

while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects:project.projectId});
    var states = [];
    while (sites.hasNext()) {
        var site = sites.next();
        if (site.extent && site.extent.geometry) {
            var state = site.extent.geometry.state;
            if (state && states.indexOf(state) < 0) {
               states.push(state);
            }
        }
    }


    var statesCsv = '';
    var max = Math.max(3, states.length);
    for (var i=0; i<max; i++) {
        statesCsv += ',' + (states[i] || '');
    }
    print('"'+project.name+'",'+project.grantId+','+project.externalId+','+project.organsationName+','+project.associatedProgram+','+project.associatedSubProgram+','+project.plannedStartDate+','+project.plannedEndDate+','+project.funding+','+project.status+',"'+project.description+'",'+statesCsv);


}