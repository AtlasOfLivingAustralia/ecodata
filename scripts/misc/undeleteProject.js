// Attempt to restore a project that was deleted.
var project = db.project.find({externalId:'MEC2_NY_030'}).next();
print(project.projectId);

db.project.update({projectId:project.projectId}, {$set:{status:'active'}});

var sites = db.auditMessage.find({entityType:'au.org.ala.ecodata.Site', 'entity.projects':project.projectId});
while (sites.hasNext()) {
    var am = sites.next();

    var site = db.site.find({siteId:am.entity.siteId}).next();

    print("Found site: "+site.siteId);
    var projects = [project.projectId];

    db.site.update({siteId:site.siteId}, {$set:{status:'active', projects: projects}});

}

var activities = db.activity.find({projectId:project.projectId});
while(activities.hasNext()) {
    var activity = activities.next();
    print("Found activity: "+activity.activityId);
    db.activity.update({activityId:activity.activityId}, {$set:{status:'active'}});

    var outputs = db.output.find({activityId:activity.activityId});
    while (outputs.hasNext()) {
        var output= outputs.next();
        print("Found output: "+output.outputId);
        db.output.update({outputId:output.outputId}, {$set:{status:'active'}});
    }
}

var documents = db.document.find({projectId:project.projectId});
while(documents.hasNext()) {
    var document = documents.next();
    print("Found document: "+document.documentId);

    db.document.update({documentId:document.documentId}, {$set:{status:'active'}});
}

db.userPermission.update({entityId:project.project}, {$set:{status:'active'}});