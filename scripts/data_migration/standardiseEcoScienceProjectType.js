/**
 * This function standardises Eco Science project type. Two project type was used previously - 'ecoscience' and 'ecoScience'.
 * @type {*}
 */

var projects = db.project.find({projectType:"ecoscience"});
print("Number of projects to change - " + projects.count());
while (projects.hasNext()) {
    var project = projects.next();
    db.project.update({projectId:project.projectId},{$set:{projectType:"ecoScience"}});
    print(project.projectId);
}