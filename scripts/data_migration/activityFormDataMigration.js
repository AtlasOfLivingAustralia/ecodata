db.output.update({name:'Koala Sightings - Tweed '}, {$set:{name:'Koala Sightings - Tweed'}});
db.activity.update({progress:{$ne:'planned'},status:{$ne:'deleted'},projectActivityId:{$exists:false}}, {$set:{formVersion:1}}, {multi:true});
db.activity.update({status:{$ne:'deleted'},projectActivityId:{$exists:true}}, {$set:{formVersion:1}}, {multi:true});