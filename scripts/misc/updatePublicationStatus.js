// This script provides functionality to update the new field publicationStatus in all active existing projects to mark
// publicationStatus as true

print("Start to update publication status");

db.project.update(

    { status:'active'},
    { $set: { publicationStatus: true } },
    { upsert: false, multi: true }

)

print("Completed!");