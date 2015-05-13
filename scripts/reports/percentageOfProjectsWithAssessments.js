var activityType = 'Vegetation Assessment - Commonwealth government methodology';
var program = 'Biodiversity Fund';

//var program = 'Caring for Our Country 2';

var projectIdsWithForm = db.activity.distinct('projectId', {type:activityType, status:{$ne:'deleted'}, publicationStatus:'published'});


var withForm = db.project.find({associatedProgram:program, projectId:{$in:projectIdsWithForm}, status:{$ne:'deleted'}}).count();
var altogether = db.project.find({associatedProgram:program, status:{$ne:'deleted'}}).count();

print(withForm);
print(altogether);
