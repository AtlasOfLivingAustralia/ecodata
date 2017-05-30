package au.org.ala.ecodata

import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication


/** Provides an interface to the ALA Collectory web services */
class CollectoryService {

    private static final String DATA_RESOURCE_COLLECTORY_PATH = 'ws/dataResource'
    private static final String INSTITUTION_COLLECTORY_PATH = 'ws/institution'

    WebService webService
    GrailsApplication grailsApplication

    /** These are configuration options used by the Collectory to describe how to import data from MERIT / BioCollect */
    Map defaultConnectionParameters = [
            protocol:"DwC",
            url:"sftp://upload.ala.org.au:{dataProvider}/DRXXX",
            automation:false,
            csv_delimiter:',',
            csv_eol:'\n',
            csv_escape_char:'\\',
            csv_text_enclosure:'"',
            termsForUniqueKey:['occurrenceID'],
            strip:false,
            incremental:false
    ]

    /**
     * Creates a new Intitution in the collectory using the supplied properties as input.
     * @param props the properties for the new institution. (orgType, description, name, url, uid)
     * @return the created institution id or null if the create operation fails.
     */
    String createInstitution(props) {

        def collectoryProps = mapOrganisationAttributesToCollectory(props)
        def result = webService.doPost(grailsApplication.config.collectory.baseURL + INSTITUTION_COLLECTORY_PATH, collectoryProps)
        String institutionId = webService.extractIdFromLocationHeader(result)

        return institutionId
    }

    private def mapOrganisationAttributesToCollectory(props) {
        Map collectoryProps = [:]
        Map mapKeyOrganisationDataToCollectory = [
                orgType: 'institutionType',
                description: 'pubDescription',
                acronym: 'acronym',
                name: 'name',
                collectoryInstitutionId: 'uid',
                url: 'websiteUrl'
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
     * @param props the properties for the new data provider and resource.
     * @return a map containing the created data provider id and data resource id, or null.
     */
    Map createDataResource(Map props) {
        Map ids = [:]

        Map collectoryProps = mapProjectAttributesToCollectoryDataResource(props)
        ids.dataProviderId = dataProviderForProject(props)

        if (ids.dataProviderId) {
            // create a dataResource in collectory to hold project outputs
            collectoryProps.dataProvider = [uid: ids.dataProviderId]
            Map result = webService.doPost(grailsApplication.config.collectory.baseURL + DATA_RESOURCE_COLLECTORY_PATH, collectoryProps)
            if (result.error) {
                throw new Exception("Failed to create Collectory data resource: ${result.error} ${result.detail ?: ""}")
            }
            ids.dataResourceId = webService.extractIdFromLocationHeader(result)

            // Now we have an id we can create the connection properties
            Map connectionParameters = [connectionParameters:collectoryConnectionParametersForProject(props, ids.dataResourceId)]
            result = webService.doPost(grailsApplication.config.collectory.baseURL + DATA_RESOURCE_COLLECTORY_PATH+'/'+ids.dataResourceId, connectionParameters)
            if (result.error) {
                throw new Exception("Failed to create Collectory data resource connection parameters: ${result.error} ${result.detail ?: ""}")
            }

        }

        ids
    }

    /**
     * Identifies the data provider to associate with the project data resource.  Right now this is either
     * MERIT or BioCollect, but this may need to be revisited.
     */
    private String dataProviderForProject(Map project) {
        return project.isMERIT ?  grailsApplication.config.collectory.dataProviderUid.merit : grailsApplication.config.collectory.dataProviderUid.biocollect
    }

    /** The Collectory expects the upload connection parameters as a JSON encoded String */
    private String collectoryConnectionParametersForProject(Map project, String dataResourceId) {

        String dataProviderName = project.isMERIT ? "merit" : "biocollect"

        Map properties = defaultConnectionParameters.clone()
        properties.url = "sftp://upload.ala.org.au:" + dataProviderName + '/' + dataResourceId

        return (properties as JSON).toString()
    }

    /**
     * Updates the Data Resource in the collectory using the supplied properties as input.  The 'hiddenJSON' field in
     * collectory is recreated to reflect the latest project properties.
     * @param project the UPDATED project in ecodata.
     * @return void.
     */
    def updateDataResource(Map project, Map changedProperties = null) {

        if (!project.dataResourceId || project.dataResourceId == "null") {
           createDataResource(project)
        }
        else {
            def projectId = project.projectId

            Map properties = changedProperties ?: project
            Map collectoryAttributes = mapProjectAttributesToCollectoryDataResource(properties)

            // Only update if a property other than the "hiddenJSON" attribute has changed.
            if (collectoryAttributes.size() > 1) {
                Map result = webService.doPost(grailsApplication.config.collectory.baseURL + 'ws/dataResource/' + project.dataResourceId, collectoryAttributes)
                if (result.error) {
                    log.error "Error updating collectory info for project ${projectId} - ${result.error}"
                }
            }
        }
    }

    private def mapProjectAttributesToCollectoryDataResource(props) {
        def mapKeyProjectDataToCollectory = [
                description: 'pubDescription',
                manager: 'email',
                name: 'name',
                dataSharingLicense: 'licenseType',
                urlWeb: 'websiteUrl'
        ]
        def collectoryProps = [:]

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
        if (props.organisationId) {

            Organisation organisation = Organisation.findByOrganisationIdAndStatusNotEqual(props.organisationId, Status.DELETED)
            if (organisation?.collectoryInstitutionId) {
                collectoryProps.institution = [uid: organisation.collectoryInstitutionId]
            }
        }
        collectoryProps
    }

}
