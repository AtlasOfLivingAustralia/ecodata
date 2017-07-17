var count = db.organisation.find({projects:{$ne:null}}).count();
print("There are " + count + " organisations with projects field set. Removing projects field from them.");
db.organisation.update({projects:{$ne:null}}, {"$unset": {"projects":1}}, {"multi": true});
print("Completed updating these organisations.");