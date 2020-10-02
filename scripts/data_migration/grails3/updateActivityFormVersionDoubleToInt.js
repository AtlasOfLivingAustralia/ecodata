var forms = db.activityForm.find({});
print("Total Forms in DB: " + forms.count());
forms.forEach( function (form){
    if (form){
        print("Updating Form Version From Double to Int: " + form.name)
        db.activityForm.update({_id: form._id},{$set: {formVersion:NumberInt(form.formVersion), minOptionalSectionsCompleted: NumberInt(form.minOptionalSectionsCompleted)}})
    }
});

var forms2 = db.activity.find({});
print("Total Forms in DB: " + forms2.count());
forms2.forEach( function (form){
    if (form){
        if (form.formVersion){
        print("Updating Form Version From Double to Int: " + form.type)
        db.activity.update({_id: form._id},{$set: {formVersion:NumberInt(form.formVersion)}});
    }
    }
});
