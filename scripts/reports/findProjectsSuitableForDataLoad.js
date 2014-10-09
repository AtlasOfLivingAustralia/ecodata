var dt = new Date(2013,7,1);
var activities = db.activity.find({"plannedEndDate":{"$lt":dt}}).sort({'projectId':1, plannedEndDate:-1});
var toggle = false;
var prevProjectId = '';
var status = [];
var progress = [];
var project;

function printRow(project, status, progress) {

    var allPlanned = true;
    for (var i=0; i<progress.length; i++) {
        if (progress[i] != 'planned') {
            allPlanned =false;
        }
    }
    var submitted = false;
    var approved = false;
    for (var i=0; i<status.length; i++) {
        if (status[i] == 'pendingApproval') {
            submitted = true;
        }
        if (status[i] == 'published') {
            approved = true;
        }
    }

    var overallStatus = 'OK';

    var approvalStatus = 'No activities submitted or approved';
    if (submitted && approved) {
        approvalStatus = 'Some submitted, some approved.';
    }
    else if (submitted) {
        approvalStatus = 'Activities submitted for approval.';
    }
    else if (approved) {
        approvalStatus = 'Activities approved.'
    }
    if (submitted || approved || !allPlanned) {
        overallStatus = 'NOT OK for data load';
    }

    print('http://fieldcapture.ala.org.au/project/index/'+project.projectId +','+ project.grantId+','+ project.externalId+','+ project.associatedProgram+','+project.associatedSubProgram+','+(allPlanned?'All activities planned':'Not all activities planned')+','+approvalStatus+','+overallStatus);

}

while (activities.hasNext()) {
    var activity = activities.next();

    if ((activity.projectId != prevProjectId)) {

        if (status.length > 0) {
            printRow(project, status, progress);
            status = [];
            progress = [];
        }
        project = db.project.find({projectId: activity.projectId}).next();
        prevProjectId = project.projectId;
    }
    var publicationStatus = activity.publicationStatus;
    if (publicationStatus === undefined || publicationStatus == 'undefined') {
        publicationStatus = 'unapproved';
    }
    status.push(publicationStatus);
    progress.push(activity.progress);
}
printRow(project, status, progress);

