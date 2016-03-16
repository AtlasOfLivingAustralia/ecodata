package au.org.ala.ecodata

import com.mongodb.DBCursor
import com.mongodb.DBObject
import com.mongodb.QueryBuilder
import grails.validation.ValidationException
import static au.org.ala.ecodata.Status.*

/**
 * Works with Organisations, mostly CRUD operations at this point.
 */
class OrganisationService {

    /** Use to include related projects in the toMap method */
    public static final String PROJECTS = 'projects'

    static transactional = 'mongo'

    def commonService, projectService, userService, permissionService, documentService, collectoryService, messageSource

    def get(String id, levelOfDetail = [], includeDeleted = false) {
        Organisation organisation
        if (includeDeleted) {
            organisation = Organisation.findByOrganisationId(id)
        }
        else {
            organisation = Organisation.findByOrganisationIdAndStatusNotEqual(id, 'deleted')
        }
        return organisation?toMap(organisation, levelOfDetail):null
    }

    def list(levelOfDetail = []) {
        return Organisation.findAllByStatusNotEqual('deleted').collect{toMap(it, levelOfDetail)}
    }

    def create(props, boolean createCollectoryInstitution = true) {

        def organisation = new Organisation(organisationId: Identifiers.getNew(true, ''), name:props.name)

        if (createCollectoryInstitution) {
            def institutionId = collectoryService.createInstitution(props)
            if (institutionId) {
                organisation.collectoryInstitutionId = institutionId
            }
        }
        try {
            // name is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            organisation.save(failOnError: true, flush:true)
            props.remove('id')
            props.remove('organisationId')
            props.remove('collectoryInstitutionId')
            commonService.updateProperties(organisation, props)

            // Assign the creating user as an admin.
            permissionService.addUserAsRoleToOrganisation(userService.getCurrentUserDetails()?.userId, AccessLevel.admin, organisation.organisationId)

            [status:'ok',organisationId:organisation.organisationId]
        }
        catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Organisation.withSession { session -> session.clear() }
            def error = "Error creating organisation - ${e.message}"
            log.error error

            def errors = (e instanceof ValidationException)?e.errors:[error]
            return [status:'error',errors:errors]
        }
    }

    def update(String id, props) {

        def organisation = Organisation.findByOrganisationId(id)
        if (organisation) {
            try {
                String oldName = organisation.name
                getCommonService().updateProperties(organisation, props)
                if (props.name && (oldName != props.name)) {
                    projectService.updateOrganisationName(organisation.organisationId, props.name)
                }
                return [status:'ok']
            } catch (Exception e) {
                Organisation.withSession { session -> session.clear() }
                def error = "Error updating organisation ${id} - ${e.message}"
                log.error error
                if (e instanceof ValidationException) {
                    error = messageSource.getMessage(e.errors.fieldError, Locale.getDefault())
                }
                return [status:'error',errors:error]
            }
        } else {
            def error = "Error updating organisation - no such id ${id}"
            log.error error
            return [status:'error',errors:error]
        }
    }

    def delete(String id, boolean destroy) {
        def organisation = Organisation.findByOrganisationId(id)
        if (organisation) {

            // Delete any user associations or permissions associated with the organisation - how exactly will permissions work?
            //permissionService.deleteAllForOrganisation(id)

            try {
                if (destroy) {
                    organisation.delete()
                } else {
                    organisation.status = 'deleted'
                    organisation.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                Organisation.withSession { session -> session.clear() }
                def error = "Error deleting organisation ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    def toMap(Organisation org, levelOfDetail = []) {
        def dbo = org.dbo
        def mapOfProperties = dbo.toMap()

        if ('projects' in levelOfDetail) {
            mapOfProperties.projects = []
            mapOfProperties.projects += projectService.search(organisationId: org.organisationId)
            mapOfProperties.projects += projectService.search(orgIdSvcProvider: org.organisationId)
        }
        if ('documents' in levelOfDetail) {
            mapOfProperties.documents = documentService.findAllByOwner('organisationId', org.organisationId)
            mapOfProperties.links = documentService.findAllLinksByOwner('organisationId', org.organisationId)

        }

        mapOfProperties.findAll {k,v -> v != null}
    }

    /**
     * Accepts a closure that will be called once for each (not deleted) Organisation in the system,
     * passing the site (as a Map) as the single parameter.
     * Implementation note, this uses the Mongo API directly as using GORM incurs a
     * significant memory and performance overhead when dealing with so many entities
     * at once.
     * @param action the action to be performed on each Organisation.
     */
    void doWithAllOrganisations(Closure action) {
        // Due to various memory & performance issues with GORM mongo plugin 1.3, this method uses the native API.
        com.mongodb.DBCollection collection = Organisation.getCollection()
        DBObject siteQuery = new QueryBuilder().start('status').notEquals(DELETED).get()
        DBCursor results = collection.find(siteQuery).batchSize(100)

        results.each { dbObject ->
            action.call(dbObject.toMap())
        }
    }

}
