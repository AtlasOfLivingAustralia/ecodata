package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.GrailsApplication


/** Provides an interface to the ALA Collectory web services */
class CollectoryService {

    WebService webService
    GrailsApplication grailsApplication

    /**
     * Creates a new Intitution in the collectory using the supplied properties as input.
     * @param props the properties for the new institution. (orgType, description, name, url, uid)
     * @return the created institution id or null if the create operation fails.
     */
    def createInstitution(props) {

        def institutionId = null
        try {
            def collectoryProps = mapOrganisationAttributesToCollectory(props)
            def result = webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/institution/', collectoryProps)
            institutionId = webService.extractCollectoryIdFromResult(result)
        }
        catch (Exception e) {
            log.error("Error creating collectory institution - ${e.message}", e)
        }
        return institutionId
    }

    private def mapOrganisationAttributesToCollectory(props) {
        def mapKeyOrganisationDataToCollectory = [
                orgType: 'institutionType',
                description: 'pubDescription',
                acronym: 'acronym',
                name: 'name',
                collectoryInstitutionId: 'uid',
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
    def syncOrganisations(OrganisationService organisationService) {
        def errors = []
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
                    def result = organisationService.create([
                            collectoryInstitutionId: inst.uid,
                            name: inst.name,
                            description: inst.pubDescription?:"",
                            url: inst.websiteUrl?:"",
                            acronym: inst.acronym?:"",
                            sourceSystem: 'collectory'], false)
                    if (!result) {
                        errors << "Unable to create organisation for institution: ${inst.name}"
                    }
                }
            })
        }
        errors
    }

    /**
     * Creates a new Data Provider (=~ ecodata project) and Data Resource (=~ ecodata outputs)
     * in the collectory using the supplied properties as input.  Much of project meta data is
     * stored in a 'hiddenJSON' field in collectory.
     * @param id the project id in ecodata.
     * @param props the properties for the new data provider and resource.
     * @return a map containing the created data provider id and data resource id, or null.
     */
    Map createDataProviderAndResource(String id, Map props) {
        Map ids = [:]

        Map collectoryProps = mapProjectAttributesToCollectory(props)
        Map result = webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/dataProvider/', collectoryProps)
        if (result.error) {
            throw new Exception("Failed to create Collectory data provider: ${result.error} ${result.detail ?: ""}")
        }
        ids.dataProviderId = webService.extractCollectoryIdFromResult(result)

        if (ids.dataProviderId) {
            // create a dataResource in collectory to hold project outputs
            collectoryProps.remove('hiddenJSON')
            collectoryProps.dataProvider = [uid: ids.dataProviderId]
            if (props.collectoryInstitutionId) {
                collectoryProps.institution = [uid: props.collectoryInstitutionId]
            }
            result = webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/dataResource/', collectoryProps)
            if (result.error) {
                throw new Exception("Failed to create Collectory data resource: ${result.error} ${result.detail ?: ""}")
            }
            ids.dataResourceId = webService.extractCollectoryIdFromResult(result)
        }

        ids
    }

    /**
     * Updates the Data Provider (=~ ecodata project) and Data Resource (=~ ecodata outputs)
     * in the collectory using the supplied properties as input.  The 'hiddenJSON' field in
     * collectory is recreated to reflect the latest project properties.
     * @param project the UPDATED project in ecodata.
     * @return void.
     */
    def updateDataProviderAndResource(Map project) {

        def projectId = project.projectId
        try {

            webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/dataProvider/' + project.dataProviderId,
                    mapProjectAttributesToCollectory(project))
            if (project.dataResourceId)
                webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/dataResource/' + project.dataResourceId,
                        [licenseType: project.dataSharingLicense])
        } catch (Exception e ) {
            def error = "Error updating collectory info for project ${projectId} - ${e.message}"
            log.error error
        }

    }

    private def mapProjectAttributesToCollectory(props) {
        def mapKeyProjectDataToCollectory = [
                description: 'pubDescription',
                manager: 'email',
                name: 'name',
                dataSharingLicense: '', // ignore this property (mapped to dataResource)
                organisation: '', // ignore this property
                projectId: 'uid',
                urlWeb: 'websiteUrl'
        ]
        def collectoryProps = [
                api_key: grailsApplication.config.api_key
        ]
        def hiddenJSON = [:]
        props.each { k, v ->
            if (v != null) {
                def keyCollectory = mapKeyProjectDataToCollectory[k]
                if (keyCollectory == null) // not mapped to first class collectory property
                    hiddenJSON[k] = v
                else if (keyCollectory != '') // not to be ignored
                    collectoryProps[keyCollectory] = v
            }
        }
        collectoryProps.hiddenJSON = hiddenJSON
        collectoryProps
    }

}
