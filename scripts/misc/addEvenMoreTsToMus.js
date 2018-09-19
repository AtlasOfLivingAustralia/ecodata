
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
    'Amytornis dorotheae (Carpentarian Grasswren)'
];
addTsToMus(['Southern Gulf'], ts);

var programs = [
    'North East',
    'West Gippsland',
    'East Gippsland'
];

ts = [
    'Litoria verreauxii alpina (Alpine Tree Frog)',
    "Philoria frosti (Baw Baw Frog)",
    "Lobelia gelida (Snow Pratia)",
    "Xerochrysum palustre (Swamp Everlasting)",
    "Epilobium brunnescens subsp. Beaugleholei (Bog Willow-herb)"
];

addTsToMus(programs, ts);

addTsToMus(['Mackay Whitsunday'],['Natator depressus (Flatback Turtle)']);



ts = [
 'Caladenia gladiolata (Small bayonet spider orchid)',
    'Hylacola pyrrhopygia parkeri (Mount Lofty Ranges Chestnut-rumped Heathwren)'];

addTsToMus(['Adelaide and Mount Lofty Ranges'], ts);


addTsToMus(['Northern and Yorke'], ['Bettongia penicillata (Woylie)']);


ts = [
    'Olearia microdisca (Small-flowered Daisy-bush)',
    'Leionema equestre (Kangaroo Island Phebalium)'
];
addTsToMus(['Kangaroo Island'],ts);



addTsToMus(['Glenelg Hopkins'],['Glenelg Estuary and Discovery Bay Ramsar Site'], 'Ramsar');

