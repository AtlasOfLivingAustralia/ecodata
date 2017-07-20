package au.org.ala.ecodata

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.log4j.Logger

/**
 * Created by koh032 on 14/07/2017.
 */
class CheckSubmissionResultJob {

    SubmissionService submissionService

    static triggers = {
        cron name:'hourly', startDelay:1000, cronExpression: '0 0 * * * ?'
    }

    def execute() {
        if (grailsApplication.config.aekosPolling?.url) {
            log.info("****** Starting aekos submission status poll ****** " + new Date())
            submissionService.checkSubmission()
            log.info("****** Finished aekos submission status poll  ******" + new Date())
        }
    }

}
