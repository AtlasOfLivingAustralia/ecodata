package au.org.ala.ecodata

class SubmissionService {

    def commonService
    def userService

    def get(def submissionRecId) {
        def o = SubmissionRecord.findById(submissionRecId)
        if (!o) {
            return null
        }
        def map = [:]
        map = commonService.toBareMap(o)

        if (o.datasetSubmitter) {
            def user = userService.getUserForUserId(o.datasetSubmitter);
            if (user) {
                map.datasetSubmitterUser = [:]
                map.datasetSubmitterUser.userId = user.userId
                map.datasetSubmitterUser.userName = user.userName
                map.datasetSubmitterUser.displayName = user.displayName
            }
        }

        def s = SubmissionPackage.findBySubmissionRecordId(o.submissionRecordId)
        if (!s) {
            return null
        }
        map.submissionPackage = commonService.toBareMap(s)

        map
    }

    def update(def submissionRecId, def props) {
        def submissionRec = props
        def submissionPackage = props.submissionPackage

        submissionRec.remove ("datasetSubmitterUser")
        submissionRec.remove ("submissionPackage")

        def sr = SubmissionRecord.findBySubmissionRecordId(submissionRecId)
        if (!sr) {
            return null
        }
        def sp = SubmissionPackage.findBySubmissionRecordId(submissionRecId)
        if (!sp) {
            return null
        }
        commonService.updateProperties(sr, submissionRec)

        Map subPckMap = [:]
        subPckMap.putAll(submissionPackage)
        subPckMap.remove("submissionRecordId") //do not re-update submissionRecordId
        subPckMap.remove("datasetAuthors")
        subPckMap.remove("datasetContact")
        subPckMap.remove ("version")

        DatasetContact dc = new DatasetContact(submissionPackage.datasetContact)
        sp.datasetContact = dc

        List <DatasetAuthor> daList = []
        submissionPackage.datasetAuthors.each {
            DatasetAuthor da = new DatasetAuthor(it)
            daList.push(da)
        }
        sp.datasetAuthors = daList

        commonService.updateProperties(sp, subPckMap)


    }

}
