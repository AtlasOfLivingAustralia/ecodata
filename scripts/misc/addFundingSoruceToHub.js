var meritHub = db.hub.find({ urlPath:"merit"});
var fundingSource = "fundingSource"
while (meritHub.hasNext()){
    var facets = meritHub.next();
    const adminFacets = facets.adminFacets.includes(fundingSource);
    const  availableFacets = facets.availableFacets.includes(fundingSource)
    // if funding source already exist it will print as true
    print("Admin Facets: " + adminFacets)
    print("Available Facets: " + availableFacets)

    if (adminFacets === false){
        facets.adminFacets.push("fundingSource");
        print("Added Funding Source to admin Facets: "+facets.adminFacets);
        db.hub.save(facets)
    }else{
        print("Funding Source Already Exist in Admin Facets:" + facets.adminFacets);
    }

    if (availableFacets === false){
        facets.availableFacets.push("fundingSource");
        print("Added Funding Source to Available Facets: "+facets.availableFacets);
        db.hub.save(facets)
    }else{
        print("Funding source is Already Exist in Available Facets: " + facets.availableFacets )
    }

}

