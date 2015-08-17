
var protectedNaturalAssests =[ 'Natural/Cultural assets managed','Threatened Species', 'Threatened Ecological Communities',
    'Migratory Species', 'Ramsar Wetland', 'World Heritage area', 'Community awareness/participation in NRM', 'Indigenous Cultural Values',
    'Indigenous Ecological Knowledge', 'Remnant Vegetation', 'Aquatic and Coastal systems including wetlands', 'Not Applicable'];

print('Asset,NRM Region,Sub Programme,Project Count,Total')
for (var a=0; a<protectedNaturalAssests.length; a++) {
    var projects = db.project.find({
        status: {$ne: 'deleted'},
        'custom.details.objectives.rows1.assets': protectedNaturalAssests[a]
    });


    var fundingByRegion = {};
    var programs = [];
    var regions = [];
    var total = 0, count = 0;
    var subTotal = 0;

    while (projects.hasNext()) {
        var project = projects.next();

        var sites = db.site.find({projects: project.projectId});
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

        var programSubProgram = project.associatedProgram + ' - ' + project.associatedSubProgram;
        if (fundingByRegion[nrm] === undefined) {
            regions.push(nrm);
            fundingByRegion[nrm] = {};
        }
        if (programs.indexOf(programSubProgram) < 0) {
            programs.push(programSubProgram);
        }
        if (fundingByRegion[nrm][programSubProgram] === undefined) {
            fundingByRegion[nrm][programSubProgram] = {count: 0, total: 0, subTotal: 0, countByTs: 0, countByTEC: 0};
        }

        fundingByRegion[nrm][programSubProgram].count++;
        count++;
        if (project.custom.details.budget && project.custom.details.budget.overallTotal) {
            total += project.custom.details.budget.overallTotal;

            fundingByRegion[nrm][programSubProgram].total += project.custom.details.budget.overallTotal;
        }

    }



    for (var i = 0; i < regions.length; i++) {
        for (var j = 0; j < programs.length; j++) {
            if (fundingByRegion[regions[i]][programs[j]]) {
                print('"'+protectedNaturalAssests[a]+'","' + regions[i] + '","' + programs[j] + '",' + fundingByRegion[regions[i]][programs[j]].count + "," + fundingByRegion[regions[i]][programs[j]].total);
            }
        }
    }


}

