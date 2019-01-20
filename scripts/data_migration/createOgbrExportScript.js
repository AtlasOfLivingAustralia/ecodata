var reefPrograms = [
    'Demonstration/ Test Projects',
    'QRWQ Program Management',
    'Stakeholder Engagement and Communications',
'Research Development and Innovation Projects (RD&I)',
'Regional Report Cards and Partnerships',
'Policy Analysis and Support',
'Paddock to Reef Program/ Reef Report Card',
'Major Integrated Projects (MIP’s)',
'Industry BMP Programs',
'Education and Extension'];
var projects = db.project.find({status:{$ne:'deleted'}, associatedProgram:{$in:reefPrograms}});
while (projects.hasNext()) {
    var project = projects.next();
    print('./exportProject.sh ' + project.projectId);
}