db.report.remove({reportId:'9785677d-498d-422a-937d-c8c5cd63aa27'});

db.report.remove({reportId:'870be9b7-3188-41ba-aab0-4ced7bcafd75'});

db.report.update({reportId:'93550066-323f-4b51-8c05-95a6b4b3d8f2'}, {$set:{name:'Stage 2', fromDate:ISODate('2012-12-31T01:00:00Z'), toDate:ISODate('2013-07-01T02:00:00Z')}});