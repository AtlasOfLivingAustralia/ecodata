package au.org.ala.ecodata.command


import au.org.ala.ecodata.ReportService
import grails.validation.Validateable

/** Command object for the targetsReportForScore* endpoints.  This is to support GET and POST requests */
class TargetReportCommand implements Validateable {

    static constraints = {
        scoreIds nullable:true, validator: { value, targetReportCommand ->
            // Adjustment reports must reference another report
            if (!value && !targetReportCommand.scoreLabels) {
                return 'nullable'
            }
        }
        scoreLabels nullable: true, validator: { value, targetReportCommand ->
            // Adjustment reports must reference another report
            if (!value && !targetReportCommand.scoreIds) {
                return 'nullable'
            }
        }
        fq nullable: true
        query nullable: true
    }

    List<String> scoreIds
    List<String> scoreLabels
    List<String> fq
    String query = "*:*"
    boolean approvedActivitiesOnly = true

    ReportService reportService

    private List scores() {
        List scores = []
        if (scoreIds) {
            scores = reportService.findScoresByScoreId(scoreIds)
        }
        else if (scoreLabels) {
            scores = reportService.findScoresByLabel(scoreLabels)
        }
        scores
    }

    Map targetsReportForScores() {
        List scores = scores()
        def targets = reportService.outputTargetsBySubProgram([fq:fq, query:query], scores)
        def scoresReport = reportService.aggregate(fq, query, scores, null, approvedActivitiesOnly)

        def results = [scores:scoresReport, targets:targets]
        return results
    }
}
