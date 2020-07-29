var meritHub = db.hub.find({ urlPath:"merit"});
var fundingSource = "fundingSource"
while (meritHub.hasNext()){
    var adminFacets = meritHub.next();
    const funding = adminFacets.adminFacets.includes(fundingSource);
    print("Funding: " + funding)
    if (funding === false){
        adminFacets.adminFacets.push("fundingSource");
        print("Added Funding Source: "+adminFacets.adminFacets);
        db.hub.save(adminFacets)
    }else{
        print("Funding Source Already Exist:" + adminFacets.adminFacets);
    }
}

