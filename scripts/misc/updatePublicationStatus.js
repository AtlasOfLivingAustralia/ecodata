// This script provides functionality to update the new field projLifecycleStatus in all existing active biocollect
// projects to Published

print("Start to update projLifecycleStatus");

db.project.update(

    { status: 'active', isMERIT: false },
    { $set: { projLifecycleStatus: 'Published' } },
    { upsert: false, multi: true }

)

print("Completed!");