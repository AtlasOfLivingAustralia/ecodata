package au.org.ala.ecodata

import org.springframework.web.util.UriUtils

/**
 * Helper class for the generation of schemas from our models.
 */
class SchemaUrlBuilder {

    static final String SCHEMA_PATH_PREFIX = '/ws/documentation/'

    /** Prefix for URLs generated for this schema */
    def urlPrefix
    /** Used to find activities, outputs */
    def metadataService

    def config

    public SchemaUrlBuilder(config, metadataService) {
        this.metadataService = metadataService
        this.urlPrefix = config.grails.serverURL + SCHEMA_PATH_PREFIX + config.app.external.api.version
    }

    public String outputSchemaUrl(outputName) {
        return urlPrefix+'/output/'+encodeName(outputName)
    }

    public String activitySchemaUrl(activityName) {
        return urlPrefix+'/activity/'+encodeName(activityName)
    }

    public String projectActivitiesSchemaUrl() {
        return urlPrefix+'/project'
    }


    def findOutputByUrlPathSegment(pathSegment) {
        metadataService.activitiesModel().outputs.find {stripPathChars(it.name) == pathSegment}
    }

    def encodeName(name) {
        UriUtils.encodePathSegment(stripPathChars(name), 'UTF-8')
    }

    /**
     * The JSON schema code doesn't like path segments with '/' in them, even if they are encoded.
     * @param name the name to process
     * @return the name without any '/' characters.
     */
    def stripPathChars(name) {
        name.replaceAll('/', '')
    }

}
