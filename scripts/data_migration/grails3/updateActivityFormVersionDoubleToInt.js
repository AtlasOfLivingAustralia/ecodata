print("============================================================================")
print("Updating form Version in Activity Form")
var form1 = db.activityForm.find({formVersion:{$exists:true}});
while (form1.hasNext()) {
    var form = form1.next();
    form.formVersion = NumberInt(form.formVersion);
    print("forms Activity Forms : " + form.name + " Form Version: " + form.formVersion)
    db.activityForm.save(form);
}
print("============================================================================")
print("Updating form minOptionalSectionsCompleted in Activity Form")
var form2 = db.activityForm.find({minOptionalSectionsCompleted:{$exists:true}});
while (form2.hasNext()) {
    var form = form2.next();
    form.minOptionalSectionsCompleted = NumberInt(form.minOptionalSectionsCompleted);
    print("forms Activity Forms : " + form.name + " Min Optional: " + form.minOptionalSectionsCompleted)
    db.activityForm.save(form);
}

print("============================================================================")
print("Updating form Version in Activity")
var form3 = db.activity.find({formVersion:{$exists:true}});
while (form3.hasNext()) {
    var form = form3.next();
    form.formVersion = NumberInt(form.formVersion);
    print("Form Activity Type : " + form.type + " Form Version: " + form.formVersion)
    db.activity.save(form);
}

