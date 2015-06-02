//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Species'});
var projects = db.project.find({status:{$ne:'deleted'}, $or:[{'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'}, {'custom.details.objectives.rows1.assets':'Threatened Species'}]});
//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'});

//print('Name, Grant ID, External ID, Organisation Name, Programme, Sub-Programme, Planned Start Date, Planned End Date, Funding, Status, Description, State, State, State');

var fundingByRegion = {};
var programs = [];
var regions = [];
var total = 0, count = 0;
while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects:project.projectId});
    var nrm = 'unknown';
    while (sites.hasNext()) {
        var site = sites.next();
        if (site.extent && site.extent.geometry) {
            if (site.extent.geometry.nrm) {
                nrm = site.extent.geometry.nrm;
                break;
            }
        }
    }

    var programSubProgram = project.associatedProgram+' - '+project.associatedSubProgram;
    if (fundingByRegion[nrm] === undefined) {
        regions.push(nrm);
        fundingByRegion[nrm] = {};
    }
    if (programs.indexOf(programSubProgram) < 0) {
        programs.push(programSubProgram);
    }
    if (fundingByRegion[nrm][programSubProgram] === undefined) {
        fundingByRegion[nrm][programSubProgram] = {count:0, total:0};
    }

    fundingByRegion[nrm][programSubProgram].count++;
    count++;
    if (project.custom.details.budget && project.custom.details.budget.overallTotal) {
        total += project.custom.details.budget.overallTotal;
        fundingByRegion[nrm][programSubProgram].total += project.custom.details.budget.overallTotal;
    }
}


print('NRM Region,Sub Programme,Project Count,Total')
for (var i=0; i<regions.length; i++) {
    for (var j=0; j<programs.length; j++) {
        if (fundingByRegion[regions[i]][programs[j]]) {
            print('"' + regions[i] + '","' + programs[j] + '",' + fundingByRegion[regions[i]][programs[j]].count + "," + fundingByRegion[regions[i]][programs[j]].total);
        }
    }
}
print("\n");
print('Totals,,'+count+","+total);
