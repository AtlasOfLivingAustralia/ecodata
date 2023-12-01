load('../../utils/audit.js');
let adminUserId = 'system';
let sites = db.site.find({externalId:{$type:'number'}});
while (sites.hasNext()) {
    let site = sites.next();
    site.externalId = new String(site.externalId);
    db.site.replaceOne({_id:site._id}, site);

    audit(site, site.siteId, 'au.org.ala.ecodata.Site', adminUserId);
}


sites = db.site.find({externalId:{$type:'object'}});
while (sites.hasNext()) {
    let site = sites.next();
    site.externalId = null;
    db.site.replaceOne({_id:site._id}, site);

    audit(site, site.siteId, 'au.org.ala.ecodata.Site', adminUserId);
}

sites = db.site.find({externalId:{$ne:null}});
while (sites.hasNext()) {
    let site = sites.next();
    site.externalIds = [{idType:'UNSPECIFIED', externalId: site.externalId}];
    delete site.externalId;
    db.site.replaceOne({_id:site._id}, site);

    audit(site, site.siteId, 'au.org.ala.ecodata.Site', adminUserId);
}