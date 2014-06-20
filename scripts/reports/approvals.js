var projects = db.project.find({status:'active'});
print('Project ID, Grant ID'+','+  'External ID'+','+  'State'+','+  'Project approved?'+','+  'Stage Approved?');

var validStates = ["South Australia",
    "New South Wales",
    "Victoria",
    "Queensland",
    "Tasmania",
    "Australian Capital Territory",
    "Western Australia",
    "Northern Territory"];

var approvedByState = {};


while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects:project.projectId});
    var states = [];
    while (sites.hasNext()) {
        var site = sites.next();
        if (site.extent && site.extent.geometry) {
            states.push(site.extent.geometry.state);
        }
    }

    var approvedActivities = db.activity.find({projectId:project.projectId, publicationStatus:'published'}).count();
    var awaitingApproval = db.activity.find({projectId:project.projectId, publicationStatus:'pendingApproval'}).count();

    var state = '';
    for (var i=0; i<states.length; i++) {

        if (validStates.indexOf(states[i]) >= 0) {
            state = states[i];
            break;
        }
    }


    var approved = (project.planStatus == 'approved') ? 'Y': 'N';

    var stageStatus = 'Not Submitted';
    if (approvedActivities > 0 && awaitingApproval > 0) {
        stageStatus = 'Approved / Awaiting Approval';
    }
    else if (approvedActivities > 0) {
        stageStatus = 'Approved';
    }
    else if (awaitingApproval > 0) {
        stageStatus = 'Awaiting Approval';
    }

    print(project.projectId+','+project.grantId+','+ project.externalId+','+ state +','+  approved+','+  stageStatus);

}

