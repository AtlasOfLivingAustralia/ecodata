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
        if(!project.ignored){
            db.project.update({$and:[{projectId: project.projectId },{status:{$ne:'deleted'}}]},{$set: {'organisationName':ABNName}})
        }
    }
}
*/

/*
var orgsInProject = db.project.distinct('organisationId',{$and: [{"organisationId":{$ne:""}}, {isMERIT: true}]})

orgsInProject.forEach(function(orgId){
    var project = db.project.findOne({$and:[{organisationId: orgId },{isMERIT:true}]})
    var orgName = project.organisationName
    db.organisation.update({organisationId: orgId},{$set:{organisationName:orgName}})
})*/


//Fixing roll back of type ignore

for(var i=0; i<manual_check_data.length;i++){
    var ABNName = manual_check_data[i].ABNName
    for(var j=0; j < manual_check_data[i].projects.length; j++){
        var project = manual_check_data[i].projects[j]
        if(project.ignored){
            //print('replace : '+ project.projectId +' - '+ ABNName +' with original org: ' + project.organisationName )
            print(project.projectId +','+ABNName +',' + project.organisationName )
            //db.project.update({$and:[{projectId: project.projectId },{status:{$ne:'deleted'}}]},{$set: {'organisationName':project.organisationName}})
        }
    }
}

//


/*Papa.parse(data, {
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
})*/






/*
replace : eb09065e-9046-4b48-a81e-47328b8459d3 - ACT Environment and Planning Directorate - Departmental with original org: Department of Primary Industries, Parks, Water and Environment
replace : 985bfa20-3d8d-452e-a78b-7d1442b80a71 - ACT Environment and Planning Directorate - Departmental with original org: Department of Environment and Natural Resources
replace : ad20a84f-c912-47cd-bfdb-94d9b51b0abe - ACT Environment and Planning Directorate - Departmental with original org: Department of Environment and Natural Resources
replace : 5de45610-ab8f-40ff-84ab-68293ac7ed01 - ACT Environment and Planning Directorate - Departmental with original org: Department of Environment and Natural Resources
replace : da74289f-b7b8-4a8c-b688-394f955b2df4 - ACT Environment and Planning Directorate - Departmental with original org: WA Department of Parks and Wildlife
replace : aa37abc4-36dc-47c4-9d83-093b41aca0a7 - ACT Environment and Planning Directorate - Departmental with original org: WA Department of Parks and Wildlife
replace : 878acdc0-7f89-45e2-b412-83453a29359b - THE AUSTRALIAN INSTITUTE OF MARINE SCIENCE with original org: Great Barrier Reef Foundation
replace : eb09065e-9046-4b48-a81e-47328b8459d3 - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: Department of Primary Industries, Parks, Water and Environment
replace : da74289f-b7b8-4a8c-b688-394f955b2df4 - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: WA Department of Parks and Wildlife
replace : aa37abc4-36dc-47c4-9d83-093b41aca0a7 - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: WA Department of Parks and Wildlife
replace : 1757b778-33ac-452f-b139-77d5c038bc99 - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: ACT Environment and Planning Directorate - Departmental
replace : bd4e78ec-a2f5-4be5-a9a5-652d54fec890 - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: Department of Environment and Natural Resources
replace : 206ba979-06a9-4981-a605-7c1f355cfc5b - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: NQ Dry Tropics Ltd
replace : ff57ca4c-2ecc-4fae-9154-258e31b5b3b2 - DEPARTMENT OF ENVIRONMENT AND NATURAL RESOURCES with original org: Great Barrier Reef Marine Park Authority
replace : ef0592b6-ed76-46eb-ac00-174069686547 - Department of Primary Industries Parks Water and Environment with original org: Fowlers Bay Eco Park Pty Ltd
replace : 534f4fdf-e12c-4a81-bde5-3c76889b211c - Department of Primary Industries Parks Water and Environment with original org: Greening Australia Limited
replace : 30a7a147-8479-4691-aa61-d773354784b0 - Department of Primary Industries Parks Water and Environment with original org: Great Barrier Reef Marine Park Authority
replace : a1c91a60-6c1c-4caa-9346-63871e4f521a - Department of Primary Industries Parks Water and Environment with original org: Fitzroy Basin Association Inc.
replace : 1856d8ef-886b-465f-b4a8-19eb46cfd669 - FOWLERS BAY ECO PARK PTY LTD with original org: Department of Primary Industries, Parks, Water and Environment
replace : 534f4fdf-e12c-4a81-bde5-3c76889b211c - FOWLERS BAY ECO PARK PTY LTD with original org: Greening Australia Limited
replace : 30a7a147-8479-4691-aa61-d773354784b0 - FOWLERS BAY ECO PARK PTY LTD with original org: Great Barrier Reef Marine Park Authority
replace : a1c91a60-6c1c-4caa-9346-63871e4f521a - FOWLERS BAY ECO PARK PTY LTD with original org: Fitzroy Basin Association Inc.
replace : 5f92de26-1090-49c4-a1f5-33cf4f8fd16f - GREAT BARRIER REEF FOUNDATION with original org: Australian Institute of Marine Science
replace : bd4e78ec-a2f5-4be5-a9a5-652d54fec890 - GREAT BARRIER REEF MARINE PARK AUTHORITY with original org: Department of Environment and Natural Resources
replace : 206ba979-06a9-4981-a605-7c1f355cfc5b - GREAT BARRIER REEF MARINE PARK AUTHORITY with original org: NQ Dry Tropics Ltd
replace : ef0592b6-ed76-46eb-ac00-174069686547 - GREENING AUSTRALIA with original org: Fowlers Bay Eco Park Pty Ltd
replace : 1856d8ef-886b-465f-b4a8-19eb46cfd669 - GREENING AUSTRALIA with original org: Department of Primary Industries, Parks, Water and Environment
replace : a1c91a60-6c1c-4caa-9346-63871e4f521a - GREENING AUSTRALIA with original org: Fitzroy Basin Association Inc.*/
