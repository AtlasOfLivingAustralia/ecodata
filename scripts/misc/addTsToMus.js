/** Adds a priority to a management unit, defaulting to a threatened species */
function addTsToMus(managementUnit, ts, category) {
    if (!category) {
        category = 'Threatened Species';
    }

    managementUnit.forEach(function (management) {
        category.forEach(function (cat) {
            var muExist = db.managementUnit.find({name: management, "priorities.category": {$eq: cat}});
            while (muExist.hasNext()) {
                var mgUnit = muExist.next();
                ts.forEach(function (species) {
                    var isPrioritiesExist = false;
                    for (i=0; i<mgUnit.priorities.length; i++){
                     if (mgUnit.priorities[i].category === cat && mgUnit.priorities[i].priority === species ){
                         isPrioritiesExist = true
                     }
                 }
                if (isPrioritiesExist){
                    print("This " + species + " Already exist in the mangement unit: "+ management+ " under this category: " + cat)
                }else{
                     mgUnit.priorities.push({category:cat, priority: species});
                     db.managementUnit.save(mgUnit);
                    print("Saving this " + species + " to the mangement unit: "+ management+ " under this category: " + cat)

                }
                });
            }
        });
    });
}






// list of species
var ts = [
    "Phascolarctos cinereus (Koala)",
    "Petaurus australis (Yellow-Bellied Glider)",
    "Notamacropus parma (Parma Wallaby)",
    "Dasyyurus maculatus (Tiger Quoll)",
    "Pteropus poliocephalus (Grey-Headed Flying-Fox)",
    "Calyptorhynchus (Calyptorhynchus) lathami (Glossy Black-Cockatoo)",
    "Menura (Menura) novaehollandia (Superb Lyrebird)",
    "Aepyprymnus rufescens (Rufous Bettong)",
    "Planigale maculate (Common Planigale)",
    "Petaurus norfolcensis (Squirrel Glider)",
    "Petrogale pencillata (Bush-tailed rock wallaby)",
    "Phascogale tapoatafa (Brush-Tailed Phascogale)",
    "Pseudomys gracilicaudatus (Eastern Chestnut Mouse)",
    "Thylogale stigmatica (Red-legged Pademelon)",
    "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands",
    "Lowland Rainforest of Subtropical Australia",
    "Coastal Swamp Oak (Casuarina glauca) Forest of New South Wales",
    "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia",
    "Sminthopsis aitkeni (Kangaroo Island Dunnart)",
    "Tachyglossus aculeatus (KI Echidna)",
    "Stipiturus malachurus intermedius (KI Southern Emu-wren)",
    "Psophodes nigrogularis leucogaster/lashmari (KI White-bellied Whipbird)",
    "Zoothera lunulate (Bassian Thrush)"
];
var ct = ["Threatened Species", "Ramsar", "Threatened Ecological Communities", "World Heritage Sites", "Soil Quality", "Sustainable Agriculture"];  // category

var mu = ['ACT','Murray','North East', 'Riverina','South East NSW', 'East Gippsland', 'Kangaroo Island',
    'South East Queensland','North Coast', 'Northern Tablelands','Central Tablelands','Greater Sydney','Hunter'];    // management unit
addTsToMus(mu, ts, ct);
