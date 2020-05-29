// load('/Users/bai187/src/ecodata/scripts/misc/linkDocumentToReport.js')

// Work on RLP project
// projects which valid programId
//db.getCollection('project').find({programId:{$exists:true}})

// find reports list based on projectId and activityType
// order by toDate ASC
// Get name and reportId


// This project has reports starting from 'stage 2'. There are three documents having stage 3, others have no stage or link to reportId directly
// db.getCollection('report').find({$and: [{projectId: '32e3bbb5-36f8-42d6-8e2f-cbcd37863188'}, {activityType: 'RLP Output Report'}]}).sort({toDate: 1})
//

// Find documments which have projectID
// update stage number with report name in the same position
// update to reportId

//db.getCollection('document').find({projectId:'97a57212-1e53-4913-9e39-9771b6f1f0cc'})

db = db.getSiblingDB('ecodata')
var projects = db.getCollection('project').find({programId:{$exists:true}})

var logs = []
var errors = []
var warnings = []
projects.forEach(function(project) {
    var projectId = project.projectId;

    var reports = db.getCollection('report').find({$and: [{projectId: projectId}, {activityType: 'RLP Output Report'}]}).sort({toDate: 1}).toArray()
    //check if some reports have been deleted
    if (reports.length > 0) {
        //check if some reports have been deleted
        var checkingReport = reports[0]
        var reportName = checkingReport.name
        var theFirstReport = reportName.substr(reportName.length - 1)
        //Year 2018/2019 - Semester 2 Outputs Report   - if document does not have stage, do nothing.  if it has, manually update
        if (isNaN(parseInt(theFirstReport)) || parseInt(theFirstReport) ==1){
            var documents = db.document.find({$and:[{projectId: projectId}, {stage:{$exists:true}}]}).toArray()
            if(documents.length > 0){
                documents.forEach(function(document) {
                    if (document['stage'] && document['stage'] != 'undefined') {
                        var documentId = document.documentId
                        var stage = document['stage']
                        if (!isNaN(parseInt(stage))) {
                            db.document.update({documentId: documentId}, {
                                $set: {
                                    stage_check: stage,
                                    stage: reports[stage - 1].name,
                                    reportId: reports[stage - 1].reportId
                                }
                            })
                        } else {
                            warnings.push('documentId start is not integer: ' +  documentId + ' ' + stage)
                        }
                    }
                })
            }

        }else if (parseInt(theFirstReport) !=1){ // starting report is not 1
            errors.push("Report does not start from 1: " +checkingReport.reportId +" " + reportName)
        }else{
            warnings.push('projectId: ' + project.projectId +' Report Id:' + checkingReport.reportId +" " + reportName)
        }
    }
})

        //var documents = db.document.find({projectId:projectId}).toArray()

        /*    documents.forEach(function(document){
                if(document['stage'] && document['stage']!='undefined'){
                    var documentId = document.documentId
                    var stage = document['stage']
                    if(!isNaN(parseInt(stage))) {
                        logs.push(documentId + ' ' + stage + ' ' + reports[stage - 1].name)
                        db.document.update({documentId: documentId}, {
                            $set: {
                                stage_check: stage,
                                stage: reports[stage - 1].name,
                                reportId: reports[stage - 1].reportId
                            }
                        })
                    }else{
                        errors.push(documentId + ' ' + stage )
                    }
                }
            })*/




print('--------------- error ---------------------')
print(errors)
print('----------------warning---------------------------')
print(warnings)

