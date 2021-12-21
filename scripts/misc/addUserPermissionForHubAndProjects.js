//Add hub permission

var userPermission = {};
userPermission.accessLevel = 'admin';
userPermission.entityId = "80b7a1c2-667c-422b-9bd0-e445ee2d9ac8"; //hubId
userPermission.userId = '133855';
userPermission.entityType = 'au.org.ala.ecodata.Hub';
userPermission.status = "active";
db.userPermission.insert(userPermission);

//Add project permissions

var programs = db.hub.findOne({hubId:"80b7a1c2-667c-422b-9bd0-e445ee2d9ac8"}).supportedPrograms;

for (i =0;i<programs.length;i++) {
	let program = programs[i];
	var projects = db.project.find({associatedProgram: program, status:"active"},{projectId:1, _id:0});
	while (projects.hasNext()) {

		let project = projects.next();
		print(project.projectId)

		var userPermission = {};
		userPermission.accessLevel = 'admin';
		userPermission.entityId = project.projectId;
		userPermission.userId = '133855';
		userPermission.entityType = 'au.org.ala.ecodata.Project';
		userPermission.status = "active";
		db.userPermission.insert(userPermission);	
	}
}