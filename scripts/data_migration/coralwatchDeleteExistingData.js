var projectActivityId = "6355f9da-ac48-4e23-bb09-e4fdaf3e446f"

var activities = db.activity.find({projectActivityId: projectActivityId,status: {$eq: 'active'}})

while (activities.hasNext()) {
        var activity = activities.next();

	 print("Deleting activity: " + activity.activityId);


	//update outputs
	var outputs = db.output.find({activityId:activity.activityId, status: {$eq: 'active'}});

	while (outputs.hasNext()) {
   		var output = outputs.next();

		output.status = "deleted"
		db.output.save(output)
	}

	var documents = db.document.find({activityId: activity.activityId, status: {$eq: 'active'}})

	while (documents.hasNext()) {
        	var document = documents.next();
		document.status = "deleted"
		db.document.save(document)
	}

	//update comments

	var comments = db.comment.find({entityType:"Activity", entityId:activity.activityId, status: {$eq: 'active'}})

	while (comments.hasNext()) {
        	var comment = comments.next();	
		comment.status = "deleted"
		db.comment.save(comment)
	}
	
	//update activities
	activity.status = "deleted"
	db.activity.save(activity)

}


//Delete records
var records = db.record.find({projectActivityId: projectActivityId,status: {$eq: 'active'}})

while (records.hasNext()) {
	var record = records.next();
	record.status = "deleted"
	db.record.save(record)
}

//Delete existing sites
var sites = db.site.find({projects:["9c55416c-f56a-4917-a65e-da1d64a851f7"], name:{$regex:"- Australia"} ,status: {$eq: 'active'}})

while (sites.hasNext()) {
	var site = sites.next();
	print("Deleting site: " + site.name);
	if(site.name == "Project area for CoralWatch - Australia"){
		site.name= "Project area for CoralWatch"
		db.site.save(site)
	}
	else{
		site.status = "deleted"
		db.site.save(site)
	}
}
