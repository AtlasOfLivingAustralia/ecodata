//This script can be used to update the record owners (recorded by) of the uploaded records in biocollect

var activityList = db.activity.find({projectActivityId:"", status:"active"})

while (activityList.hasNext()) {
    var activity = activityList.next();

    if(activity.userId != ""){
        activity.userId = ""
    }

    db.activity.save(activity)
}

var recordList = db.record.find({projectActivityId:"", status:"active"})

while (recordList.hasNext()) {
    var record = recordList.next();

    if(record.userId != ""){
        record.userId = ""
    }

    db.record.save(record)
}