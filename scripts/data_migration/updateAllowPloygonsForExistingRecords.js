//This script is used to set two new fields: allowPolygons and allowPoints to the existing records
//For the existing records which allowAddtionalSurverySites is true, the set allowPolygons to true and allowPoints to false
// {allowPloygons:{$exists:false}} is used to double check, in case some existing surveies have been mannually updated.


db.getCollection('projectActivity').update({$and:[{allowAdditionalSurveySites:true},{allowPolygons:{$exists:false}}]},{$set:{allowPolygons:true,allowPoints:false}},{multi:true})