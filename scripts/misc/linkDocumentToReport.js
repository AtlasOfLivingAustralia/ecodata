// load('/Users/bai187/src/ecodata/scripts/misc/linkDocumentToReport.js')

// Work on RLP project
// projects which valid programId
db.getCollection('project').find({programId:{$exists:true}})

// find reports list based on projectId and activityType
// order by toDate ASC
// Get name and reportId

db.getCollection('report').find({$and:[{projectId:'97a57212-1e53-4913-9e39-9771b6f1f0cc'}, {activityType:'RLP Output Report'}]}).sort({toDate:1})

// Find documments which have projectID
// update stage number with report name in the same position
// update to reportId

