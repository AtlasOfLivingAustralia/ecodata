var outputs = db.output.find({name:'Event Details', 'data.events':{$exists:false}});
var outputCount = outputs.count();
var count = 0;
while (outputs.hasNext()) {
    var output = outputs.next();

    print("Updating event details for activity: "+output.activityId+", output: "+output.outputId);
    output.data.events= [];

    var purposes = output.data.eventPurpose;

    var hoursPerEvent = 0;
    if (output.data.eventDurationHrs && purposes.length > 0) {
        hoursPerEvent = output.data.eventDurationHrs / purposes.length;
    }


    for (var i=0; i<purposes.length; i++) {
        var event = {};
        event.eventType = 'Other (specify in notes)';
        event.eventPurpose = purposes[i];
        event.eventTopics = output.data.eventTopics;
        event.eventDurationHrs = hoursPerEvent;
        event.industryType = output.data.industryType;

        output.data.events.push(event);
    }
    output.data.deliveryPartner = "Other (specify in notes)";

    delete output.data.eventPurpose;
    delete output.data.eventTopics;
    delete output.data.eventDurationHrs;
    delete output.data.industryType;

    db.output.save(output);
    count++;
}
if (outputCount != count) {
    print("Error! Expected "+outputCount+" but modified "+count+" Event Details outputs");
}
else {
    print("Updated "+count+" Event Details outputs");
}