var projects = db.project.find({
    status: {$ne: 'deleted'},
    associatedProgram: 'National Landcare Programme',
    associatedSubProgram: 'Regional Funding'
});

print();

var fys = ['2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019'];

var header = 'Grant ID, External ID, Name, Organisation, MERI Plan Status, Label, Description, 2013/2014, 2014/2015, 2015/2016, 2016/2017, 2017/2018, 2018/2019, Partner name, Partner Description, Partner Category, Partner name, Partner Description, Partner Category, ';

print(header);
var missingCount = 0;
var count = 0;
var total = 0;
while (projects.hasNext()) {
    count++;
    var project = projects.next();
    if (project.custom && project.custom.details && project.custom.details.budget) {


        for (var j = 0; j < project.custom.details.budget.rows.length; j++) {

            var row = project.grantId + ',' + project.externalId + ',"' + project.name +'",' + project.organisationName + ',' + project.planStatus;
            var budgetRow = project.custom.details.budget.rows[j];
            row += ',"' + budgetRow.shortLabel + '",' + '"' + budgetRow.description + '"';
            for (var yr = 0; yr < fys.length; yr++) {

                row += ',';
                var found = false;
                for (var k = 0; k < project.custom.details.budget.headers.length; k++) {

                    if (project.custom.details.budget.headers[k].data == fys[yr]) {
                        found = true;

                        row += project.custom.details.budget.rows[j].costs[k].dollar;

                        break;
                    }
                }

            }
            row += getPartners(project);
            print(row);

        }
        total += project.custom.details.budget.overallTotal;
    }
    else {
        var row = project.grantId + ',' + project.externalId + ',"' + project.name + '",' + project.organisationName + ',' + project.planStatus+',,,,,,,,'+getPartners(project);
        print(row);
        missingCount++;
    }


}
print('Count=' + count);
print('Total=' + total);
print('Missing count=' + missingCount);


function getPartners(project) {
    var partnerDetails = '';
    if (project.custom.details.partnership && project.custom.details.partnership.rows) {
        for (var i=0; i<project.custom.details.partnership.rows.length; i++) {
            var partner = project.custom.details.partnership.rows[i];
            partnerDetails += ',"'+partner.data1+'","'+(partner.data2?partner.data2.replace(/"\n/g, ""):'')+'","'+partner.data3+'"';
        }
    }
    return partnerDetails;
}