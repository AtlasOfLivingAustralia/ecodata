//copy none nlp and elp programs to management unit
db.program.find({$nor:[{"name": "National Landcare Programme"},{name:"Regional Land Partnerships"}]}).forEach(function(program){
    if (program.name != 'Marine Natural Resource Management') {
        db.getCollection("managementUnit").insert(program);
    }

    db.program.remove({programId:program.programId});
})
//rename Ids
//db.managementUnit.updateMany({},{$rename:{'programId':'managementUnitId','programSiteId':'managementUnitSiteId','config.programReports':'config.managementUnitReports'}})

// Update the cooperative management unit outcomes (which are missing a shortDescription)
var actMu = db.managementUnit.find({name:'ACT'}).next();
var outcomes = actMu.outcomes;

db.managementUnit.update({name:'Co-operative Management Area'}, {$set:{outcomes:outcomes}});

var mus = db.managementUnit.find({});
while (mus.hasNext()) {
    var mu = mus.next();
    db.managementUnit.update({_id:mu._id},{$rename:{'programId':'managementUnitId','programSiteId':'managementUnitSiteId','config.programReports':'config.managementUnitReports'}})
}



//update programID to managementUnit id
db.project.find({'programId':{$exists:true}}).forEach(function(project){
    db.project.update( {"projectId":project.projectId}, {$set:{"managementUnitId": project.programId}})
})



//Update RPL programId
var rlp_programId = db.program.findOne({name:"Regional Land Partnerships"}).programId

//db.project.updateMany({'grantId':{$regex:/^RLP/}},{$set:{'programId':rlp_programId}})

db.project.find({programId:{$exists:true}}).forEach(function(project){
    db.project.update( {"projectId":project.projectId}, {$set:{'programId':rlp_programId}})
})



//Update ERF programId
var erf_program = db.program.findOne({name:"Environmental Restoration Fund"})
if (erf_program){
    //db.project.updateMany({'grantId':{$regex:/^ERF/}},{$set:{'programId':erf_program.programId}})
    db.project.find({'grantId':{$regex:/^ERF/}}).forEach(function(project){
        db.project.update( {"projectId":project.projectId}, {$set:{'programId':erf_program.programId}})
    })
}else{
    load('uuid.js');
    var now = ISODate();
    var programStart = ISODate('2019-06-30T14:00:00Z');
    var programEnd = ISODate('2023-06-30T13:59:59Z');
    var parentId = db.program.find({name: "National Landcare Programme"}).next()._id;

    var erf_program = {
        name: "Environmental Restoration Fund",
        programId: UUID.generate(),
        dateCreated: now,
        lastUpdated: now,
        startDate: programStart,
        parent: parentId,
        endDate: programEnd,
        status: 'active',
        config: {
            "meriPlanTemplate": "rlpMeriPlan",
            "projectTemplate": "rlp",
            "activityPeriodDescriptor": "Outputs report #",
            "requiresActivityLocking": true,
            "navigationMode": "returnToProject",
            "visibility": "public",
            "riskAndThreatTypes": [
                "Performance",
                "Work Health and Safety",
                "People resources",
                "Financial",
                "External stakeholders",
                "Natural Environment"
            ]
        }
    };

    db.program.insert(erf_program)
    //db.project.updateMany({'grantId':{$regex:/^ERF/}},{$set:{'programId':erf_program.programId}})
    db.project.find({'grantId':{$regex:/^ERF/}}).forEach(function(project){
        db.project.update( {"projectId":project.projectId}, {$set:{'programId':erf_program.programId}})
    })
}

db.program.update({}, {$set:{outcomes:outcomes}}, {multi:true});

// Update permissions associated with programs
db.userPermission.update({entityType:'au.org.ala.ecodata.Program'}, {$set:{entityType:'au.org.ala.ecodata.ManagementUnit'}}, {multi:true});

db.report.update({programId:{$exists:true}}, {$rename:{'programId':'managementUnitId'}}, {multi:true});

// Update the MERIT hub to add new facets
var hub = db.hub.find({urlPath:'merit'}).next();
hub.availableFacets.push("primaryOutcomeFacet");
hub.availableFacets.push("secondaryOutcomesFacet");
hub.availableFacets.push("muFacet");
hub.availableMapFacets.push("primaryOutcomeFacet");
hub.availableMapFacets.push("muFacet");

db.hub.save(hub);