
load('uuid.js');

var projects = db.project.find({isMERIT:true, status:{$ne:'deleted'}, programId:{$exists:true}});

while (projects.hasNext()) {
    var project = projects.next();
    print("Processing project: "+project.projectId);
    var auditMessages = db.auditMessage.find({projectId:project.projectId, entityType:'au.org.ala.ecodata.Project'}).sort({date:1});

    var previousPlanStatus = 'not approved';
    while (auditMessages.hasNext()) {
        var message = auditMessages.next();

        if (previousPlanStatus != 'approved' && message.entity.planStatus == 'approved') {
            // Found an approval.
            print("Found approval for "+project.projectId+" at "+message.date);

            var date = new Date();
            var name = project.grantId+' MERI plan approved - ';
            var filename = 'meri-approval-'+project.projectId+"-"+message.date.getTime()+'.txt';

            var basePath = '/data/ecodata/uploads/';
            var filepath = '2019-07';

            var document = {
                role:'approval',
                name:name,
                filename:filename,
                public:false,
                readOnly:true,
                type:'text',
                contentType:'application/json',
                dateCreated:date,
                lastUpdated:date,
                projectId:message.projectId,
                status:"active",
                documentId:UUID.generate(),
                filepath:filepath,
                labels:['MERI']
            };

            var content = {
                dateApproved:message.date,
                approvedBy:message.userId,
                reason:'',
                comment:'',
                project:message.entity
            };

            print(basePath+filepath+'/'+filename);

            //printjson(document);
            var existing = db.document.find({filename:filename});
            if (existing.hasNext()) {
                print("Already migrated "+filename);
            }
            else {
               // writeFile(basePath+filepath+'/'+filename, JSON.stringify(content));
                db.document.insert(document);
            }

        }
        previousPlanStatus = message.entity.planStatus;

    }



}
