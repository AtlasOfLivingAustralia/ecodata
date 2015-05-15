package au.org.ala.ecodata


/** Provides an interface to the ALA Collectory web services */
class CollectoryService {

    def webService, grailsApplication

    /**
     * Creates a new Intitution in the collectory using the supplied properties as input.
     * @param props the properties for the new institution. (orgType, description, name, url, uid)
     * @return the created institution id or null if the create operation fails.
     */
    def createInstitution(props) {

        def institutionId = null
        try {
            def collectoryProps = mapAttributesToCollectory(props)
            def result = webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/institution/', collectoryProps)
            institutionId = webService.extractCollectoryIdFromResult(result)
        }
        catch (Exception e) {
            log.error("Error creating collectory institution - ${e.message}", e)
        }
        return institutionId
    }

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
}
