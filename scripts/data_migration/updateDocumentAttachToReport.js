print('starting to update document stage to report')
var projects = db.project.find({isMERIT: true});
print('Find ' + projects.size() + " projects")
while(projects.hasNext()){
    var project = projects.next();
    var documents = db.document.find({projectId: project.projectId})

    var reports = db.report.find({projectId: project.projectId}).toArray()
    print(reports.length + ' reports in this project.')

    while(documents.hasNext()){
        var document = documents.next();
        var stage = document.stage;
        if (stage){
            print('Replacing document '+ document.name +" Stage:" + stage)
            //find the first report of current stage
            var report = reports.find(function(report){return report.name === 'Stage ' +stage  || report.description.startsWith('Stage ' +stage )})
            if(report){
                print("With report Id: " + report.reportId)
                //document.reportId = report.reportId
                db.document.update({'_id': document._id},{$set:{reportId: report.reportId}})

            }else{
                print("Cannot find the report")
            }
        }
    }
}
