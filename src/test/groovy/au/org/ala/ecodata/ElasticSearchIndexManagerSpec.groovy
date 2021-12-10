package au.org.ala.ecodata

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.GetAliasesResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.rest.RestStatus
import spock.lang.Specification

import static au.org.ala.ecodata.ElasticIndex.DEFAULT_INDEX
import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX
import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX

class ElasticSearchIndexManagerSpec extends Specification {

    Map mapping = [:]
    Map settings = [:]
    String defaultPrefix = "development"
    ElasticSearchIndexManager elasticSearchIndexManager
    RestHighLevelClient client = GroovyMock(RestHighLevelClient)
    IndicesClient indicesClient = GroovyMock(IndicesClient)

    def setup() {
        elasticSearchIndexManager = new ElasticSearchIndexManager(client, defaultPrefix, settings, mapping)
        client.indices() >> indicesClient
    }


    def "the ElasticSearchIndexManager will manage two indexes for each alias using prefixes and suffixes"(String alias, List expected) {
        expect:
        elasticSearchIndexManager.indexNamesForAlias(alias) == expected

        where:
        alias                               | expected
        ElasticIndex.HOMEPAGE_INDEX         | ["development_homepage_1", "development_homepage_2"]
        ElasticIndex.DEFAULT_INDEX          | ["development_search_1", "development_search_2"]
        ElasticIndex.PROJECT_ACTIVITY_INDEX | ["development_pasearch_1", "development_pasearch_2"]
    }

    def "initialisation checks that indexes have been created and that aliases are setup"() {
        setup:
        List allIndexes = [
                "development_homepage_1", "development_homepage_2",
                "development_search_1", "development_search_2",
                "development_pasearch_1", "development_pasearch_2"]
        List allAliases = [DEFAULT_INDEX, HOMEPAGE_INDEX, PROJECT_ACTIVITY_INDEX]

        when:
        elasticSearchIndexManager.initialiseIndexAliases()

        then: "All required indexes have been created"
        allIndexes.each {String index ->
            1 * indicesClient.exists({
                it.indices()[0] in allIndexes
            }, RequestOptions.DEFAULT) >> true
        }
        0 * indicesClient.create(_,_)

        allAliases.each {String alias ->
            1 * indicesClient.getAlias({it.aliases() == [alias]}, RequestOptions.DEFAULT) >> buildAliasCorrectResponse(alias)
        }

        0 * indicesClient.getAlias(_,_)
        0 * indicesClient.updateAliases(_, _)
    }

    def "indexes that don't exist at startup will be created"() {
        setup:
        List allIndexes = [
                "development_search_1", "development_search_2",
                "development_homepage_1", "development_homepage_2",
                "development_pasearch_1", "development_pasearch_2"]
        List allAliases = [DEFAULT_INDEX, HOMEPAGE_INDEX, PROJECT_ACTIVITY_INDEX]
        allAliases.each {String alias ->
            indicesClient.getAlias({it.aliases() == [alias]}, RequestOptions.DEFAULT) >> buildAliasCorrectResponse(alias)
        }

        when:
        elasticSearchIndexManager.initialiseIndexAliases()

        then: "All required indexes have been created"
        allIndexes.each {String index ->
            1 * indicesClient.exists({
                it.indices()[0] in allIndexes
            }, RequestOptions.DEFAULT) >> false
        }
        allIndexes.each { String index ->
            1 * indicesClient.create({ it.index == index }, RequestOptions.DEFAULT)
        }
    }

    def "when switching environments, the alias will be switched"() {
        setup:
        List allIndexes = [
                "development_homepage_1", "development_homepage_2",
                "development_search_1", "development_search_2",
                "development_pasearch_1", "development_pasearch_2"]
        List allAliases = [DEFAULT_INDEX, HOMEPAGE_INDEX, PROJECT_ACTIVITY_INDEX]

        when:
        elasticSearchIndexManager.initialiseIndexAliases()

        then: "All required indexes have been created"
        allIndexes.each {String index ->
            1 * indicesClient.exists({
                it.indices()[0] in allIndexes
            }, RequestOptions.DEFAULT) >> true
        }
        0 * indicesClient.create(_,_)

        allAliases.each {String alias ->
            1 * indicesClient.getAlias({it.aliases() == [alias]}, RequestOptions.DEFAULT) >> buildAliasResponse(alias, "test alias")
        }

        allAliases.each {String alias ->
            1 * indicesClient.getAlias({it.aliases() == [alias]}, RequestOptions.DEFAULT) >> buildAliasResponse(alias, "test alias")
        }
        allAliases.each { String alias ->
            1 * indicesClient.updateAliases({verifyUpdateAliasRequest(alias, it, "test alias", defaultPrefix+"_"+alias+"_1")}, RequestOptions.DEFAULT) >> AcknowledgedResponse.of(true)
        }
    }

    def "the alias can be switched"(String alias, String existingIndex, String newIndex) {
        setup:

        when:
        elasticSearchIndexManager.updateAlias(alias, newIndex)

        then:
        1 * indicesClient.getAlias({it.aliases() == [alias]}, RequestOptions.DEFAULT) >> buildAliasResponse(alias, existingIndex)
        1 * indicesClient.updateAliases({verifyUpdateAliasRequest(alias, it, existingIndex, newIndex)}, RequestOptions.DEFAULT) >> AcknowledgedResponse.of(true)

        where:
        alias                               | existingIndex            | newIndex
        ElasticIndex.PROJECT_ACTIVITY_INDEX | "development_homepage_1" | "development_homepage_2"
        ElasticIndex.PROJECT_ACTIVITY_INDEX | "development_homepage_2" | "development_homepage_1"

    }

    private boolean verifyUpdateAliasRequest(String alias, IndicesAliasesRequest request, String indexToRemove, String indexToAdd) {
        verifyAll {
            request.aliasActions.size() == 2
            request.aliasActions[0].aliases()[0] == alias
            request.aliasActions[0].indices()[0] == indexToRemove
            request.aliasActions[0].actionType() == IndicesAliasesRequest.AliasActions.Type.REMOVE

            request.aliasActions[1].aliases()[0] == alias
            request.aliasActions[1].indices()[0] == indexToAdd
            request.aliasActions[1].actionType() == IndicesAliasesRequest.AliasActions.Type.ADD
        }
        true
    }

    private GetAliasesResponse buildAliasCorrectResponse(String aliasName) {
        String indexName = defaultPrefix+"_"+aliasName+"_1"
        buildAliasResponse(aliasName, indexName)
    }

    private GetAliasesResponse buildAliasResponse(String aliasName, String indexName) {
        Map metadata = [(indexName):new HashSet()]
        new GetAliasesResponse(RestStatus.OK, null, metadata)
    }


}
