// load('/Users/bai187/src/ecodata/scripts/misc/updateProjectOrg.js')

load("/Users/bai187/src/ecodata/scripts/lib/papaparse.min.js");
load("/Users/bai187/src/ecodata/scripts/lib/updateOrgData.js")

var notMatchedProjects = []

/*Papa.parse(data, {
    delimiter: "\t",
    header: true,
    complete: function (result) {
        var data = result.data
        print("Data:" + data.length);
        for (var i = 0; i < data.length; i++) {
            var record = data[i];
            if (record["Project IDs"] && record['Matching name']) {
                var givenIDs = record["Project IDs"].split(';').filter(Boolean);

                var org = record['Matching name'].replace(new RegExp("'", "g"),"\\'")

                givenIDs.forEach(function (id) {
                    try {
                        var projects = db.project.find({$and: [{externalId: id}, {status: {$ne: 'deleted'}}]}, {
                            projectId: 1,
                            externalId: 1,
                            organisationName: 1,
                            organisationId: 1,
                            status: 1
                        })
                        var count = projects.count()
                        if (count == 1) {
                            //update
                            db.project.update({$and: [{externalId: id}, {status: {$ne: 'deleted'}}]}, {$set: {'organisationName': org}})
                        } else if (count == 0) {
                            //Try grant ID
                            var projects = db.project.find({$and: [{grantId: id}, {status: {$ne: 'deleted'}}]}, {
                                projectId: 1,
                                externalId: 1,
                                organisationName: 1,
                                organisationId: 1,
                                status: 1
                            })
                            var pNum = projects.count()

                            if (pNum > 1) {
                                //mulitple projects, has to do manual check
                                notMatchedProjects.push({
                                    orgInExcel: record['Organisation Names'],
                                    idInExcel: id,
                                    ABNName: record['Matching name'],
                                    projects: projects.toArray()
                                });
                            } else if (pNum == 1) {
                                var num = db.project.update({$and: [{grantId: id}, {status: {$ne: 'deleted'}}]}, {$set: {'organisationName': org}})
                                if (num.nMatched != 1) {
                                    print(num.nMatched + ' updated on grant Id: ' + id + ' with org: ' + org)
                                }
                            } else {
                                print('NO record found on grant Id: ' + id + ' with org: ' + org)
                            }
                        } else {
                            notMatchedProjects.push({
                                cid: record['Organisation Names'] + ' ' + id,
                                orgInExcel: record['Organisation Names'],
                                idInExcel: id,
                                ABNName: org,
                                projects: projects.toArray()
                            });
                        }
                    }catch(e){
                        print(e)
                    }

                })
            }
        }
    }
})*/

/*var result = notMatchedProjects.filter((obj, pos, arr) => {
    return arr.map(mapObj => mapObj['cid']).indexOf(obj['cid']) === pos ;
})

print(result)*/




/*
for(var i=0; i<manual_check_data.length;i++){
    var ABNName = manual_check_data[i].ABNName
    for(var j=0; j < manual_check_data[i].projects.length; j++){
        var project = manual_check_data[i].projects[j]
        if(!project.igored){
            db.project.update({$and:[{projectId: project.projectId },{status:{$ne:'deleted'}}]},{$set: {'organisationName':ABNName}})
        }
    }
}
*/


Papa.parse(data, {
    delimiter: "\t",
    header: true,
    complete: function (result) {
        var data = result.data
        print("Data:" + data.length);
        for (var i = 0; i < data.length; i++) {
            var record = data[i];
            var targetOrg =  record['Organisation Names'];
            var ABNName = record['Matching name'];
            var abn =  record['ABN'];
            db.organisation.update({name: targetOrg},{$set: {'name':ABNName, 'ABN':abn}})
        }
    }
})



/*
var orgsInProject = db.project.distinct('organisationId',{$and: [{"organisationId":{$ne:""}}, {isMERIT: true}]})

orgsInProject.forEach(function(orgId){
    var project = db.project.findOne({$and:[{organisationId: orgId },{isMERIT:true}]})
    var orgName = project.organisationName
    db.organisation.update({organisationId: orgId},{$set:{organisationName:orgName}})
})*/
