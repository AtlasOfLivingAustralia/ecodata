var count = db.site.find({'extent.geometry.pid':'null'}).count();
print("Number of sites to update with pid value 'null' - " + count);
db.site.update({'extent.geometry.pid':'null'},{$set:{'extent.geometry.pid':null}},{multi:true});
print("Completed!");