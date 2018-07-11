load('uuid.js');
load('rlpScores.js');

var sortedScores = db.score.find({}).sort({_id:-1});
var maxId = sortedScores.next()._id;

for (var i=0; i<scores.length; i++) {
    var score = scores[i];

    score.scoreId = UUID.generate();
    score._id = maxId+i+1;
    db.score.insert(score);
}