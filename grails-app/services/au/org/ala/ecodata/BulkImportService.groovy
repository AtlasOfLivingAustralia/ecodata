package au.org.ala.ecodata

import au.org.ala.web.AuthService
import grails.web.databinding.DataBinder

class BulkImportService implements DataBinder {
    static final DETAILS_MINIMAL = 'minimal'
    CommonService commonService
    ProjectActivityService projectActivityService
    AuthService authService

    Map list(Map filters, Map options, String searchTerm){
        def criteria = BulkImport.createCriteria()
        Closure action = {
            filters?.each { prop, value ->
                if (value instanceof List) {
                    inList(prop, value)
                } else {
                    eq(prop, value)
                }
            }

            if (searchTerm) {
                or {
                    eq('bulkImportId', searchTerm)
                    eq('projectId', searchTerm)
                    eq('userId', searchTerm)
                    eq('projectActivityId', searchTerm)
                }
            }
        }

        def results = criteria.list(options, action)
        def imports = results.collect {
            toMap(it, DETAILS_MINIMAL)
        }

        [total: results.getTotalCount(), items: imports]
    }

    BulkImport create(Map content) {
        BulkImport bulkImport = new BulkImport(content)
        bulkImport.bulkImportId = Identifiers.getNew(true, null)
        bindData(bulkImport, content, [include:BulkImport.bindingProperties])
        bulkImport.save(flush: true)
        bulkImport
    }

    Map update(props) {
        try {
            BulkImport bulkImport = BulkImport.findByBulkImportId(props.bulkImportId)
            bindData(bulkImport, props, [include:BulkImport.bindingProperties])
            bulkImport.save(flush: true)
            return [status: 'ok', bulkImportId: bulkImport.bulkImportId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            BulkImport.withSession { session -> session.clear() }
            def error = "Error updating bulk import ${props.bulkImportId} - ${e.message}"
            log.error error, e

            return [status: 'error', error: error]
        }
    }

    Map toMap (BulkImport bulkImport, levelOfDetails ) {
        def map = bulkImport.toMap()
        if (levelOfDetails == DETAILS_MINIMAL) {
            map.removeAll {key, value -> key in ['dataToLoad', 'createdActivities', 'validActivities', 'invalidActivities', 'id'] }

            if(map.projectActivityId) {
                def pa = ProjectActivity.findByProjectActivityId(map.projectActivityId)
                map.projectActivityName = pa?.name
            }

            if (map.userId) {
                def user = authService.getUserForUserId(map.userId)
                map.userName = user?.displayName
            }

            if(map.projectId) {
                def project = Project.findByProjectId(map.projectId)
                map.projectName = project?.name
            }
        }
        map

    }
}
