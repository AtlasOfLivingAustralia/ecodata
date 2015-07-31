//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Species'});
var projects = db.project.find({status:{$ne:'deleted'}, $or:[{'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'}, {'custom.details.objectives.rows1.assets':'Threatened Species'}]});
//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'});

//print('Name, Grant ID, External ID, Organisation Name, Programme, Sub-Programme, Planned Start Date, Planned End Date, Funding, Status, Description, State, State, State');


var fundingByRegion = {};
var programs = [];
var regions = [];
var total = 0, count = 0;
var subTotal = 0;

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
        fundingByRegion[nrm][programSubProgram] = {count:0, total:0, subTotal:0, countByTs:0, countByTEC:0};
    }

    fundingByRegion[nrm][programSubProgram].count++;
    count++;
    if (project.custom.details.budget && project.custom.details.budget.overallTotal) {
        total += project.custom.details.budget.overallTotal;

        fundingByRegion[nrm][programSubProgram].total += project.custom.details.budget.overallTotal;

        subTotal = 0;
        for (var i=0; i<project.custom.details.budget.rows.length; i++) {
            if (project.custom.details.budget.rows[i].shortLabel === 'Communities are protecting species and natural assets') {
                subTotal += project.custom.details.budget.rows[i].rowTotal;
            }
        }
        fundingByRegion[nrm][programSubProgram].subTotal += subTotal;
    }

    var foundTEC = false, foundTS = false;
    for (var i=0; i<project.custom.details.objectives.rows1.length; i++) {
        for (var j=0; j<project.custom.details.objectives.rows1[i].assets.length; j++) {
            if (project.custom.details.objectives.rows1[i].assets[j] === 'Threatened Ecological Communities') {
                foundTEC = true;
            }
            if (project.custom.details.objectives.rows1[i].assets[j] === 'Threatened Species') {
                foundTS = true;
            }
        }
    }
    if (foundTEC) {
        fundingByRegion[nrm][programSubProgram].countByTEC++;
    }
    if (foundTS) {
        fundingByRegion[nrm][programSubProgram].countByTs++;
    }


}


print('NRM Region,Sub Programme,Project Count,Total,Towards communities are protecting species and natural assests,Projects addressing Threatened Ecological Communities,Projects Addressing Threatened Species')
for (var i=0; i<regions.length; i++) {
    for (var j=0; j<programs.length; j++) {
        if (fundingByRegion[regions[i]][programs[j]]) {
            print('"' + regions[i] + '","' + programs[j] + '",' + fundingByRegion[regions[i]][programs[j]].count + "," + fundingByRegion[regions[i]][programs[j]].total +','+ fundingByRegion[regions[i]][programs[j]].subTotal+','+ fundingByRegion[regions[i]][programs[j]].countByTEC+','+ fundingByRegion[regions[i]][programs[j]].countByTs);
        }
    }
}
print("\n");
print('Totals,,'+count+","+total);

