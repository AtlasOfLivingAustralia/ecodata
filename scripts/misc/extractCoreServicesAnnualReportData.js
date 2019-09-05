
var coreServicesAnnualReports = db.report.find({activityType:'RLP Core Services annual report', toDate:{$lte:ISODate('2019-07-02T00:00:00Z')}});

var count = 0;

print("Management Unit, URL, From Date, To Date, Approval Status, 9a, 9b");
while (coreServicesAnnualReports.hasNext()) {
    var report = coreServicesAnnualReports.next();

    var mu = db.program.find({programId:report.programId}).next();


    var fromDate = report.fromDate;
    var toDate = report.toDate;

    var output = db.output.find({activityId:report.activityId});

    var q9a = '';
    var q9b = '';
    if (output.hasNext()) {
        var outputData = output.next();

        q9a = outputData.data && outputData.data.indigenousParticipation;
        q9b = outputData.data && outputData.data.indigenousParticipationForClause40Point2;
    }

    q9a = (q9a || '').replace("\"", "'").replace("\n", " ");
    q9b = (q9b || '').replace("\"", "'").replace("\n", " ");;

    //printjson(output.data);

    print(mu.name+",https://fieldcapture.ala.org.au/rlp/index/"+mu.programId+","+report.fromDate+","+report.toDate+","+report.publicationStatus+",\""+q9a+"\",\""+q9b+"\"");


    count++;
}

print(count);