
var projects = db.project.find({status:{$ne:'deleted'}});

print('Name, Grant ID, External ID, Organisation Name, Programme, Sub-Programme, Planned Start Date, Planned End Date, Funding, Status, Description, Electorate, Electorate, Electorate, Electorate, Electorate, Electorate, Electorate, Electorate, Electorate, Electorate');

while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects:project.projectId});
    var electorates = [];
    while (sites.hasNext()) {
        var site = sites.next();
        if (site.extent && site.extent.geometry) {
            electorates.push(site.extent.geometry.electorate);
        }
    }

    var electoratesCsv = '';
    var max = Math.max(10, electorates);
    for (var i=0; i<max; i++) {
        electoratesCsv += ',' + (electorates[i] || '');
    }
    print('"'+project.name+'",'+project.grantId+','+project.externalId+','+project.organsationName+','+project.associatedProgram+','+project.associatedSubProgram+','+project.plannedStartDate+','+project.plannedEndDate+','+project.funding+','+project.status+',"'+project.description+'",'+electoratesCsv);


}