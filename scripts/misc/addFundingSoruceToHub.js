var meritHub = db.hub.find({ urlPath:"merit"});
var fundingSource = "fundingSource"
var fundings = "fundingSourceFacet"
while (meritHub.hasNext()){
    var facets = meritHub.next();
    const adminFacets = facets.adminFacets.includes(fundingSource);
    const  availableFacets = facets.availableFacets.includes(fundingSource);
    const adminFacets2 = facets.adminFacets.includes(fundings);
    const  availableFacets2 = facets.availableFacets.includes(fundings);
    // if funding source already exist it will print as true
    print("Admin Facets: " + adminFacets)
    print("Available Facets: " + availableFacets)
    print("Admin Facets2: " + adminFacets2)
    print("Available Facets2: " + availableFacets2)

    if (adminFacets === true){
        facets.adminFacets.pop("fundingSource");
        print("Remove Funding Source to admin Facets: " + adminFacets)
        db.hub.save(facets);
    }
    if (availableFacets === true){
        facets.availableFacets.pop("fundingSource")
        print("Remove Funding Source to Available Facets: "+facets.availableFacets);
        db.hub.save(facets)
    }

    if (adminFacets2 === false){
        facets.adminFacets.push("fundingSourceFacet");
        print("Added Funding Source to admin Facets: "+facets.adminFacets);
        db.hub.save(facets)
    }else{
        print("Funding Source Already Exist in Admin Facets:" + facets.adminFacets);
    }

    if (availableFacets2 === false){
        facets.availableFacets.push("fundingSourceFacet");
        print("Added Funding Source to Available Facets: "+facets.availableFacets);
        db.hub.save(facets)
    }else{
        print("Funding source is Already Exist in Available Facets: " + facets.availableFacets )
    }

}

