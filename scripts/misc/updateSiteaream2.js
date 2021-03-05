/*
*when user upload a sites in MERIT spatial portal returning the right calculated area but when storing in ecodata the value is reduced by 10000 for this specific upload
*
* @ Thakur
*  */

db.site.update({siteId: "a02911dc-c1a7-4df5-adee-b3844033fd36"}, {$set:{"extent.geometry.aream2": 1149952789.34}});
print("Site are updated");

