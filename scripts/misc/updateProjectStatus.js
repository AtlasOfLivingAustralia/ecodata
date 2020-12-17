// This script provide functionality to update the project status of the projects in MERIT which don't have a MERI plan
// This change is done under https://github.com/AtlasOfLivingAustralia/fieldcapture/issues/2099

print("Start to update project status");

db.project.update(

    { $and: [ { planStatus:"not approved" }, { status:'active'} , { origin:"merit" }, { custom: null }] },
    { $set: { status: "application" } },
    { upsert: false, multi: true  }

)

print("Completed!");