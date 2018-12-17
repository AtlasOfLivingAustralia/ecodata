
function addTsToMus(programs, ts, category) {
    if (!category) {
        category = 'Threatened Species';
    }
    for (var i=0; i<programs.length; i++) {
        mu = db.program.find({name:programs[i]});
        if (mu.count() != 1) {
            throw "Wrong count for MU: " + programs[i] +", count="+mu.count();
        }
        m = mu.next();
        for (var j=0; j<ts.length; j++) {
            m.priorities.push({category:category, priority: ts[j]});
        }
        db.program.save(m);
        print("Saving program:"+m.name);

    }
}

var ts = [
    'Litoria raniformis (Growling Grass Frog)'
];
addTsToMus(['North Central'], ts);


ts = [
    'Tachyglossus aculeatus multiaculeatus (Kangaroo Island echidna)'
];

addTsToMus(['Kangaroo Island'], ts);