/**
 * converts scienceType property of Project from String to a List
 * @type {*|{}}
 */

var projects = db.project.find({scienceType:{$ne:null}})
while (projects.hasNext()) {
    var project = projects.next();
    if(typeof project.scienceType == 'string'){
        db.project.update({projectId:project.projectId},{$set:{scienceType:[project.scienceType]}})
        print(project.projectId)
    }
}