var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Species'});
//var projects = db.project.find({status:{$ne:'deleted'}, $or:[{'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'}, {'custom.details.objectives.rows1.assets':'Threatened Species'}]});
//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'});

//print('Name, Grant ID, External ID, Organisation Name, Programme, Sub-Programme, Planned Start Date, Planned End Date, Funding, Status, Description, State, State, State');

var fundingByRegion = {};
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

    if (fundingByRegion[nrm] === undefined) {
        regions.push(nrm);
        fundingByRegion[nrm] = {count:0, total:0};
    }

    fundingByRegion[nrm].count++;
    count++;
    if (project.custom.details.budget && project.custom.details.budget.overallTotal) {
        total += project.custom.details.budget.overallTotal;
        fundingByRegion[nrm].total += project.custom.details.budget.overallTotal;
    }


}


print('NRM Region,Project Count,Total')
for (var i=0; i<regions.length; i++) {
    print('"'+regions[i]+'",'+fundingByRegion[regions[i]].count+","+fundingByRegion[regions[i]].total);
}
print("\n");
print('Totals,'+count+","+total);
