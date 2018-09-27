db.score.remove({category:'RLP', label:'Area (ha) protected by fire management action'});

db.score.update({category:'RLP', label:'Area (ha) fire management action implemented'}, {$set:{label:'Area (ha) treated by fire management action'}});

db.score.update({category:'RLP', label:'Number of structures in place to manage water'}, {$set:{label:'Number of treatments implemented to improve water management'}});

db.score.remove({category:'RLP', label:'Area (ha) of catchment being managed as a result of this management action'});

db.score.remove({category:'RLP', label:'Area (ha) surveyed for skills and knowledge'});

db.score.update({category:'RLP', label:'Area of feral-free enclosure'}, {$set:{label:'Area (ha) of feral-free enclosure'}});

db.score.update({category:'RLP', label:'Number of ex-situ breeding sites and/or populations'}, {$set:{label:'Number of breeding sites and/or populations'}});


