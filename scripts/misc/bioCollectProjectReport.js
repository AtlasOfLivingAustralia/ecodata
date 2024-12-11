var projects = db.project.find({isMERIT: false, status: {$ne: 'deleted'}}),
    project;
var headers = ["Project start date", "Project end date", "Project type", "Project name", "Project status",
    "Count of number of members/participants (including casual project participants) for each project",
    "Count of number of surveys for each project",
    "Count of number of event records/activities for each project",
    "Count of the occurrence records (total) for each project"]

print(headers.join(","));
while (projects.hasNext()) {
    project = projects.next();
    var columns = []

    columns.push('"' + getDate(project.plannedStartDate) + '"');
    columns.push('"' + getDate(project.plannedEndDate) + '"');
    columns.push('"' + project.projectType + '"');
    columns.push('"' + project.name.replace('"', '""') + '"' );
    columns.push('"' + project.status + '"');
    columns.push(getMembersOfProjectCount(project));
    columns.push(getSurveysCount(project));
    columns.push(getActivitiesCount(project) );
    columns.push(getOccurrencesCount(project));
    print(columns.join(","));
}

function getMembersOfProjectCount(project) {
    return db.userPermission.find({entityId: project.projectId, entityType: "au.org.ala.ecodata.Project", status: {$ne: 'deleted'}}).count();
}

function getSurveysCount(project) {
    return db.projectActivity.find({projectId: project.projectId, status: {$ne: 'deleted'}}).count();
}

function getActivitiesCount(project) {
    return db.activity.find({projectId: project.projectId, status: {$ne: 'deleted'}}).count();
}

function getOccurrencesCount(project) {
    return db.record.find({projectId: project.projectId, status: {$ne: 'deleted'}}).count();
}

function getDate(date){
    if (date) {
        var eventDate = date;
        return eventDate.getDate() + '/' + (eventDate.getMonth() + 1) + '/' + eventDate.getFullYear();
    }
    else return "";
}