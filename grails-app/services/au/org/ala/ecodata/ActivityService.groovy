package au.org.ala.ecodata

class ActivityService {

    static transactional = false

    def grailsApplication

    def getCommonService() {
        grailsApplication.mainContext.commonService
    }

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param act an Activity instance
     * @return map of properties
     */
    def toMap(act) {
        def dbo = act.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.remove("outputs")
        mapOfProperties.outputs = act.outputs.collect {
            [outputId: it.outputId,
                    assessmentDate: it.assessmentDate,
                    collector: it.collector]
        }
        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadAll(list) {
        list.each {
            create(it)
        }
    }

    def create(props) {
        assert getCommonService()
        def site = Site.findBySiteId(props.siteId)
        if (site) {
            def o = new Activity(siteId: site.siteId, activityId: Identifiers.getNew(true,''))
            try {
                getCommonService().updateProperties(o, props)
                site.addToActivities(o)
                site.save()
                return [status:'ok',activityId:o.activityId]
            } catch (Exception e) {
                // clear session to avoid exception when GORM tries to autoflush the changes
                Activity.withSession { session -> session.clear() }
                def error = "Error creating activity for site ${props.siteId} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error creating activity - no site with id = ${props.siteId}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def a = Activity.findByActivityId(id)
        if (a) {
            try {
                getCommonService().updateProperties(a, props)
                return [status:'ok']
            } catch (Exception e) {
                Activity.withSession { session -> session.clear() }
                def error = "Error updating Activity ${id} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error updating Activity - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

}
