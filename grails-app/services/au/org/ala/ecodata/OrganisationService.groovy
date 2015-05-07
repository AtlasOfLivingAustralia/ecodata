package au.org.ala.ecodata

import grails.validation.ValidationException

/**
 * Works with Organisations, mostly CRUD operations at this point.
 */
class OrganisationService {

    /** Use to include related projects in the toMap method */
    public static final String PROJECTS = 'projects' //

    static transactional = 'mongo'

    def grailsApplication, webService, commonService, projectService, permissionService, documentService

    private def mapAttributesToCollectory(props) {
        def mapKeyOrganisationDataToCollectory = [
                orgType: 'institutionType',
                description: 'pubDescription',
                name: 'name',
                organisationId: 'uid',
                url: 'websiteUrl'
        ]
        def collectoryProps = [
                api_key: grailsApplication.config.api_key
        ]
        props.each { k, v ->
            if (v != null) {
                def keyCollectory = mapKeyOrganisationDataToCollectory[k]
                if (keyCollectory) collectoryProps[keyCollectory] = v
            }
        }
        collectoryProps
    }

    // create ecodata organisations for any institutions in collectory which are not yet in ecodata
    // return null if sucessful, or errors
    def collectorySync() {
        def errors
        def url = "${grailsApplication.config.collectory.baseURL}ws/institution/"
        def institutions = webService.getJson(url)
        if (institutions instanceof List) {
            def orgs = Organisation.findAllByCollectoryInstitutionIdIsNotNull()
            def map = orgs.collectEntries {
               [it.collectoryInstitutionId, it]
            }
            institutions.each({it ->
                if (!map[it.uid]) {
                    def inst = webService.getJson(url + it.uid)
                    def result = create([collectoryInstitutionId: inst.uid,
                                        name: inst.name,
                                        description: inst.pubDescription?:"",
                                        url: inst.websiteUrl?:""])
                    if (result.errors) errors = result.errors
                }
            })
        }
        errors
    }

    def get(String id, levelOfDetail = [], includeDeleted = false) {
        Organisation organisation
        if (includeDeleted) {
            organisation = Organisation.findByOrganisationIdIlike(id)
        }
        else {
            organisation = Organisation.findByOrganisationIdAndStatusNotEqual(id, 'deleted')
        }
        return organisation?toMap(organisation, levelOfDetail):null
    }

    def list(levelOfDetail = []) {
        return Organisation.findAllByStatusNotEqual('deleted').collect{toMap(it, levelOfDetail)}
    }

    def create(props) {

        def organisation = new Organisation(organisationId: Identifiers.getNew(true, ''), name:props.name)
        try {
            def collectoryProps = mapAttributesToCollectory(props)
            def result = webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/institution/', collectoryProps)
            organisation.collectoryInstitutionId = webService.extractCollectoryIdFromResult(result)
        }
        catch (Exception e) {
            def error = "Error creating collectory institution - ${e.message}"
        }
        try {
            // name is a mandatory property and hence needs to be set before dynamic properties are used (as they trigger validations)
            organisation.save(failOnError: true, flush:true)
            props.remove('id')
            commonService.updateProperties(organisation, props)

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
                if (organisation.name != props.name)
                    projectService.updateOrgName(organisation.organisationId, props.name)
                getCommonService().updateProperties(organisation, props)
                return [status:'ok']
            } catch (Exception e) {
                Organisation.withSession { session -> session.clear() }
                def error = "Error updating organisation ${id} - ${e.message}"
                log.error error
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            def error = "Error updating organisation - no such id ${id}"
            log.error error
            return [status:'error',errors:[error]]
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
            mapOfProperties.projects = projectService.search(organisationId: org.organisationId)
        }
        if ('documents' in levelOfDetail) {
            mapOfProperties.documents = documentService.findAllByOwner('organisationId', org.organisationId)
        }

        mapOfProperties.findAll {k,v -> v != null}
    }

}
