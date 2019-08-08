//mongo ecodata addShortDescriptionToOutcomes.js
var rlp = db.program.find({name:"Test program"}).next();

var programs = db.program.find({parent:rlp._id, status:{$ne:'deleted'}});

while (programs.hasNext()) {
    var program = programs.next();

    for (var i=0; i<program.outcomes.length; i++) {
        var outcome = program.outcomes[i];

        if (outcome.outcome == "By 2023, there is restoration of, and reduction in threats to, the ecological character of Ramsar sites, through the implementation of priority actions") {
            outcome.shortDescription = "Ramsar Sites";
        }
        else if (outcome.outcome == "2. By 2023, the trajectory of species targeted under the Threatened Species Strategy, and other EPBC Act priority species, is stabilised or improved.") {
            outcome.shortDescription = "Threatened Species Strategy";
        }
        else if (outcome.outcome == "3. By 2023, invasive species management has reduced threats to the natural heritage Outstanding Universal Value of World Heritage properties through the implementation of priority actions.") {
            outcome.shortDescription = "World Heritage Areas";
        }
        else if (outcome.outcome == "4. By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities."){
            outcome.shortDescription = "Threatened Ecological Communities"
        }
        else if (outcome.outcome == "5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.") {
            outcome.shortDescription = "Soil Condition";
        }
        else if (outcome.outcome == "6. By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production.") {
            outcome.shortDescription = "Climate / Weather Adaption";
        }

        db.program.save(program);
    }
}