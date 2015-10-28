var projects = db.project.find({status:{$ne:'deleted'}, associatedProgram:'National Landcare Programme', associatedSubProgram:'Regional Funding'});

print();

var fys = ['2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019'];

var header = 'Grant ID, External ID, MERI Plan Status, Label, Description, 2013/2014, 2014/2015, 2015/2016, 2016/2017, 2017/2018, 2018/2019';

print(header);

while (projects.hasNext()) {
    var project = projects.next();
    if (project.custom && project.custom.details && project.custom.details.budget) {


        for (var j=0; j<project.custom.details.budget.rows.length; j++) {

            var row = project.grantId+','+project.externalId+','+project.planStatus;
            var budgetRow = project.custom.details.budget.rows[j];
            row+=',"'+budgetRow.shortLabel+'",'+'"'+budgetRow.description+'"';
            for (var yr=0; yr<fys.length; yr++) {

                row+=','
                var found = false;
                for (var k = 0; k< project.custom.details.budget.headers.length; k++ ) {

                    if ( project.custom.details.budget.headers[k].data == fys[yr]) {
                        found = true;

                        row+=project.custom.details.budget.rows[j].costs[k].dollar;

                        break;
                    }
                }

            }
            print(row);

        }
    }
    else {
        var row = project.grantId+','+project.externalId+','+project.planStatus;
        print(row);
    }


}
