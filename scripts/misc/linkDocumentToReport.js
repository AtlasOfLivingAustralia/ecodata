// load('/Users/bai187/src/ecodata/scripts/misc/linkDocumentToReport.js')

// Work on RLP project
// projects which valid programId
//db.getCollection('project').find({programId:{$exists:true}})

// find reports list based on projectId and activityType
// order by toDate ASC
// Get name and reportId

//db.getCollection('report').find({$and:[{projectId:'97a57212-1e53-4913-9e39-9771b6f1f0cc'}, {activityType:'RLP Output Report'}]}).sort({toDate:1})

// Find documments which have projectID
// update stage number with report name in the same position
// update to reportId

//db.getCollection('document').find({projectId:'97a57212-1e53-4913-9e39-9771b6f1f0cc'})

db = db.getSiblingDB('ecodata')
var projects = db.getCollection('project').find({programId:{$exists:true}})

projects.forEach(function(project){
    var projectId = project.projectId;

    var reports = db.getCollection('report').find({$and:[{projectId: projectId}, {activityType:'RLP Output Report'}]}).sort({toDate:1}).toArray()
     var documents = db.document.find({projectId:projectId}).toArray()

    documents.forEach(function(document){
        if(document['stage'] && document['stage']!='undefined'){
            var documentId = document.documentId
            var stage = document['stage']
            print(documentId + ' ' + stage + ' ' + reports[stage-1].name )
            db.document.update({documentId: documentId},{$set:{stage_check: stage,stage:reports[stage-1].name, reportId:reports[stage-1].reportId}})
        }
    })
  }
)

