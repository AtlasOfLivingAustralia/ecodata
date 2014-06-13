var types = [];
types.push('Feral animal assessment');
types.push('Infrastructure Establishment');
types.push('Public Access Management');
types.push('Vegetation Assessment - BioCondition (QLD)');
types.push('Vegetation Assessment - BioMetric (NSW)');
types.push('Vegetation Assessment - Bushland Condition Monitoring (SA)');
types.push('Vegetation Assessment - Habitat Hectares (VIC)');
types.push('Vegetation Assessment - Native Vegetation Condition Assessment and Monitoring (WA)');
types.push('Vegetation Assessment - TasVeg (TAS)');
types.push('Feral animal assessment');
types.push('Biodiversity Fund - Outcomes and Monitoring');

print('Activity Type, Project ID, Activity ID,Has outputs?');
for (var i = 0; i<types.length; i++) {
    var type = types[i];
    var activities = db.activity.find({type: type});
    var count = 0;
    var noOutputCount = 0;
    while (activities.hasNext()) {
        var activity = activities.next();
        var outputs = db.output.find({activityId: activity.activityId}).count();
        if (outputs > 0) {
            print(type+','+activity.projectId+','+activity.activityId+',Yes');
            count++;
        }
        else {
            noOutputCount++;

        }
    }
    //print('****' + count + '/' + noOutputCount + '***');
}

var activities = db.activity.find({type:'Site assessment'});
while (activities.hasNext()) {
    var activity = activities.next();
    var outputs = db.output.find({activityId: activity.activityId}).count();
    if (outputs > 0) {
        print(type+','+activity.projectId+','+activity.activityId+',Yes');
        count++;
    }
    else {
        print(type+','+activity.projectId+','+activity.activityId+',No');
    }
}


// Pest management with disease component.
var outputs = db.output.find({name:'Pest Management Details', 'data.pestManagementMethod':/disease/});
while (outputs.hasNext()) {
    var output = outputs.next();
    var activity = db.activity.find({activityId:output.activityId}).next();

    print('Pest and Disease Management,',activity.projectId+','+output.activityId+',Yes');
}

// Site planning details with deleted output options.
var outputs = db.output.find({name:'Site Planning Details', 'data.plannedActions.plannedActivityType':/Indigenous|Knowledge Integration and Transfer|Evaluation and Learning|Training and Skills Development|Management Plan Development|Project Administration|Site Monitoring Planning/});
while (outputs.hasNext()) {
    var output = outputs.next();
    var activity = db.activity.find({activityId:output.activityId}).next();
    print('Site Planning Details,',activity.projectId+','+output.activityId+',Yes');
}

// accessControlDetails
var outputs = db.output.find({name:'Access Control Details', 'data.structuresInstalled':{$exists:true}});
while (outputs.hasNext()) {
    var output = outputs.next();
    var activity = db.activity.find({activityId:output.activityId}).next();
    print('Public Access Management,',activity.projectId+','+output.activityId+',Yes');
}