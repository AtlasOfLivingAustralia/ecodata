/** Adds a priority to a management unit, defaulting to a threatened species */
function addTsToMus(managementUnit, ts, category) {
    if (!category) {
        category = 'Threatened Species';
    }

    managementUnit.forEach(function (management) {
            var muExist = db.managementUnit.find({name: management, "priorities.category": {$eq: category}});
            while (muExist.hasNext()) {
                var mgUnit = muExist.next();
                ts.forEach(function (species) {
                    var isPrioritiesExist = false;
                    for (i=0; i<mgUnit.priorities.length; i++){
                     if (mgUnit.priorities[i].category === category && mgUnit.priorities[i].priority === species ){
                         isPrioritiesExist = true
                     }
                 }
                if (isPrioritiesExist){
                    print("This " + species + " Already exist in the mangement unit: "+ management+ " under this category: " + category)
                }else{
                     mgUnit.priorities.push({category:category, priority: species});
                     db.managementUnit.save(mgUnit);
                    print("Saving this " + species + " to the mangement unit: "+ management+ " under this category: " + category)

                }
                });
            }
    });
}






// list of species
var ts = [
    "Phascolarctos cinereus (Koala)",
    "Petaurus australis (Yellow-Bellied Glider)",
    "Notamacropus parma (Parma Wallaby)",
    "Dasyyurus maculatus (Tiger Quoll)",
    "Pteropus poliocephalus (Grey-Headed Flying-Fox)",
    "Aepyprymnus rufescens (Rufous Bettong)",
    "Planigale maculate (Common Planigale)",
    "Petaurus norfolcensis (Squirrel Glider)",
    "Petrogale pencillata (Bush-tailed rock wallaby)",
    "Phascogale tapoatafa (Brush-Tailed Phascogale)",
    "Pseudomys gracilicaudatus (Eastern Chestnut Mouse)",
    "Thylogale stigmatica (Red-legged Pademelon)",
    "Sminthopsis aitkeni (Kangaroo Island Dunnart)",
    "Tachyglossus aculeatus (KI Echidna)",
    "Stipiturus malachurus intermedius (KI Southern Emu-wren)",
    "Psophodes nigrogularis leucogaster/lashmari (KI White-bellied Whipbird)",
    "Zoothera lunulate (Bassian Thrush)"
];
var tsCat = "Threatened Species";  // category

var TEC = [
    "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands",
    "Lowland Rainforest of Subtropical Australia",
    "Coastal Swamp Oak (Casuarina glauca) Forest of New South Wales",
    "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia",
];
var TECCat = "Threatened Ecological Communities" //Threatened Ecological Communities category

var mu = ['ACT','Murray','North East', 'Riverina','South East NSW', 'East Gippsland', 'Kangaroo Island',
    'South East Queensland','North Coast', 'Northern Tablelands','Central Tablelands','Greater Sydney','Hunter'];    // management unit
addTsToMus(mu, ts, tsCat);
addTsToMus(mu, TEC, TECCat);
