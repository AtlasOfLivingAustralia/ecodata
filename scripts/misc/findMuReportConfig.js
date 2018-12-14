var mus = db.program.find({status:{$ne:'deleted'}});
while (mus.hasNext()) {

    var mu = mus.next();
    if (mu.config) {

        var outputReportConfig = {};
        var programReportConfig = {};

        if (mu.config.projectReports) {
            for (var i=0; i<mu.config.projectReports.length; i++) {
                if (mu.config.projectReports[i].activityType =='RLP Output Report') {
                    outputReportConfig = mu.config.projectReports[i];
                }
            }
            if (outputReportConfig) {
                if (outputReportConfig.firstReportingPeriodEnd == '2018-09-30T14:00:00Z' && outputReportConfig.reportingPeriodInMonths == 3) {
                    outputReportConfig.group = 'Quarterly - Group C (First period ends 30 September 2018)'
                }
                else if (outputReportConfig.firstReportingPeriodEnd == '2018-10-31T13:00:00Z' && outputReportConfig.reportingPeriodInMonths == 3) {
                    outputReportConfig.group = 'Quarterly - Group D (First period ends 31 October 2018)'
                }
                else if (outputReportConfig.firstReportingPeriodEnd == '2019-02-28T13:00:00Z' && outputReportConfig.reportingPeriodInMonths == 6) {
                    outputReportConfig.group = 'Half-yearly - Group E (First period ends 28 February 2019)'
                }
                else if (outputReportConfig.firstReportingPeriodEnd == '2019-03-31T13:00:00Z' && outputReportConfig.reportingPeriodInMonths == 6) {
                    outputReportConfig.group = 'Half-yearly - Group F (First period ends 31 March 2019)'
                }
            }
        }

        if (mu.config.programReports) {
            for (var i=0; i<mu.config.programReports.length; i++) {
                if (mu.config.programReports[i].activityType =='RLP Core Services report') {
                    programReportConfig = mu.config.programReports[i];
                }
            }

            if (programReportConfig) {
                if (programReportConfig.firstReportingPeriodEnd == '2018-07-31T14:00:00Z' && programReportConfig.reportingPeriodInMonths == 1) {
                    programReportConfig.group = 'Monthly (First period ends 31 July 2018)'
                }
                else if (programReportConfig.firstReportingPeriodEnd == '2018-08-31T14:00:00Z' && programReportConfig.reportingPeriodInMonths == 2) {
                    programReportConfig.group = 'Bi-monthly (First period ends 31 August 2018)'
                }
                else if (programReportConfig.firstReportingPeriodEnd == '2018-09-30T14:00:00Z' && programReportConfig.reportingPeriodInMonths == 3) {
                    programReportConfig.group = 'Quarterly - Group A (First period ends 30 September 2018)'
                }
                else if (programReportConfig.firstReportingPeriodEnd == '2018-08-31T14:00:00Z' && programReportConfig.reportingPeriodInMonths == 3) {
                    programReportConfig.group = 'Quarterly - Group B (First period ends 31 August 2018)'
                }
                else if (programReportConfig.firstReportingPeriodEnd == '2019-01-31T13:00:00Z' && programReportConfig.reportingPeriodInMonths == 6) {
                    programReportConfig.group = 'Half-yearly (First period ends 31 January 2019)'
                }
            }
        }


        print(mu.programId+','+'"'+mu.name+'",'+outputReportConfig.group+','+outputReportConfig.reportingPeriodInMonths+','+outputReportConfig.firstReportingPeriodEnd+','+programReportConfig.group+','+programReportConfig.reportingPeriodInMonths+','+programReportConfig.firstReportingPeriodEnd);

    }

}