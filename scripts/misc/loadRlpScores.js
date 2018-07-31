load('uuid.js');
load('rlpScores.js');

var sortedScores = db.score.find({}).sort({_id:-1});
var maxId = sortedScores.next()._id;

for (var i=0; i<scores.length; i++) {
    var score = scores[i];

    var existing = db.score.find({label:score.label});
    if (existing.hasNext()) {
        if (existing.count() > 1) {
            throw "Duplicate score label found: "+score.label;
        }

        else {

            print("Updating score: "+score.label);
            var existingScore = existing.next();
            score._id = existingScore._id;
            score.scoreId = existingScore.scoreId;
            db.score.save(existingScore);
        }

    }
    else {
        print("Inserting score: "+score.label);
        score.scoreId = UUID.generate();
        score._id = maxId+i+1;
        db.score.insert(score);
    }


}