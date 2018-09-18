

var ts = [
    'Thinornis rubricollis (Hooded plover)',
    'Grevillea treueriana (Mount Finke grevillea)',
    'Hibbertia crispula (Ooldea guinea flower)',
    'Calidris tenuirostris (Great knot)',
    'Calidris ferruginea (Curlew sandpiper)',
    'Calidris canutus (Red knot)',
    'Limosa lapponica (Bar-tailed godwit)'
];

var mu = db.program.find({name:'Alinytjara Wilurara'});
if (mu.count() != 1) {
    throw "Wrong count for MU: Alinytjara Wilurara, count="+mu.count();
}

var m = mu.next();
for (var i=0; i<ts.length; i++) {
    m.priorities.push({category:'Threatened Species', priority: ts[i]});
}
db.program.save(m);

ts = ['Euastacus dharawalus (Fitzroy Falls Spiny Crayfish)'];
mu = db.program.find({name:'South East NSW'});
if (mu.count() != 1) {
    throw "Wrong count for MU:South East NSW, count="+mu.count();
}

var m = mu.next();
for (var i=0; i<ts.length; i++) {
    m.priorities.push({category:'Threatened Species', priority: ts[i]});
}
db.program.save(m);

