
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
    'Manorina melanotis (Black-eared Miner)',
    'Pachycephala rufogularis (Red-lored Whistler)',
    'Polytelis anthopeplus monarchoides (Regent Parrot)',
    'Psophodes leucogaster leucogaster (Western Whipbird)',
    'Stipiturus mallee (Mallee Emu-wren)'
];
addTsToMus(['South Australian Murray Darling Basin'], ts);


ts = [
    'Elseya albagula (White-throated Snapping Turtle)',
    'Rheodytes leukops (Fitzroy River Turtle)'
];

addTsToMus(['Fitzroy'], ts);