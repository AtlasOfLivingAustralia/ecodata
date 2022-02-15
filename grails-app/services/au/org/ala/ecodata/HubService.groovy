package au.org.ala.ecodata

import grails.validation.ValidationException
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

import static au.org.ala.ecodata.Status.DELETED

@Slf4j
class HubService {

    static transactional = 'mongo'
    def grailsApplication
    MessageSource messageSource
    CommonService commonService
    PermissionService permissionService
    CacheService cacheService

    Hub get(String id, includeDeleted = false) {

        Hub hub
        if (!includeDeleted) {
            hub = Hub.findByHubIdAndStatusNotEqual(id, DELETED)
        }
        else {
            hub = Hub.findByHubId(id)
        }
        hub
    }

    Map findByUrlPath(String id) {
        Hub hub = Hub.findByUrlPathAndStatusNotEqual(id, DELETED)
        hub ? toMap(hub) : null
    }

    Map create(props) {

        Hub hub = new Hub(hubId: Identifiers.getNew(true, ''), urlPath:props.urlPath)

        try {
            props.remove('urlPath')
            props.remove('hubId')

            commonService.updateProperties(hub, props)
            // urlPath is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            hub.save(failOnError: true, flush:true)

            [status :'ok',hubId:hub.hubId]
        }
        catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Hub.withSession { session -> session.clear() }
            def error = "Error creating hub: ${hub.urlPath} - ${e.message}"
            log.error error

            def errors = (e instanceof ValidationException)?e.errors:[error]
            return [status:'error',errors:errors]
        }
    }

    Map update(String id, props) {

        Hub hub = get(id, false)
        if (hub) {
            try {
                commonService.updateProperties(hub, props)
                return [status:'ok']
            } catch (Exception e) {
                Hub.withSession { session -> session.clear() }
                def error = "Error updating hub ${hub.urlPath} - ${e.message}"
                log.error error
                if (e instanceof ValidationException) {
                    error = messageSource.getMessage(e.errors.fieldError, Locale.getDefault())
                }
                return [status:'error',errors:[error]]
            }
        } else {
            def error = "Error updating hub - no such urlPath ${props.urlPath}"
            log.error error
            return [status:'error',errors:[error]]
        }
    }

    Map delete(String id, boolean destroy) {
        Hub hub = get(id)
        if (hub) {

            try {
                if (destroy) {
                    hub.delete()
                } else {
                    hub.status = DELETED
                    hub.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                Hub.withSession { session -> session.clear() }
                def error = "Error deleting hub ${hub.urlPath} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id: '+id]]
        }
    }

    Map toMap(Hub hub) {
        Map properties = commonService.toBareMap(hub)
        // Don't include the user details lookup as some hubs (e.g. MERIT have hundreds of users with access
        // and this slows the call down a lot, and this information is not required as we are using this
        // as an ACL only
        properties.userPermissions = permissionService.getMembersForHub(hub.hubId, false)
        properties
    }

    /** Returns a list of hubs which have a non-zero value for one of the accessManagementOptions */
    List<Hub> findHubsEligibleForAccessExpiry() {
        Hub.where {
            accessManagementOptions.warnUsersAfterPeriodInactive != null ||
                    accessManagementOptions.expireUsersAfterPeriodInactive != null

        }.list()
    }

    List<String> findBioCollectHubs() {
        cacheService.get('biocollect-hubs', { ->
            def meritHub = grailsApplication.config.getProperty('app.hub.merit')
            Hub.findAllByUrlPathNotEqual(meritHub).collect { it.urlPath }
        })
    }

}
