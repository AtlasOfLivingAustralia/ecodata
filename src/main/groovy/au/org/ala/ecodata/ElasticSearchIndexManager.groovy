package au.org.ala.ecodata

import groovy.util.logging.Slf4j
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.GetAliasesResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.rest.RestStatus

import static au.org.ala.ecodata.ElasticIndex.*

/**
 * This class is responsible for managing the indexes used by ecodata, and the aliases that point to those
 * indexes.
 * Two sets of indexes are maintained to allow for the system to be re-indexed then swap over to the new
 * index to avoid the system presenting incorrect results to users during the reindexing process.
 * The other problem this class attempts to solve is to preserve indexed data when a developer switches
 * between running integration/functional tests and running ecodata in development mode.  This is done
 * by prefixing the index names per-environment and managing the aliases to point to the correct index, thus
 * preventing integration tests from overwriting the main index.
 */
@Slf4j
class ElasticSearchIndexManager {

    private static final List<String> ALL_INDEX_TYPES = [DEFAULT_INDEX, HOMEPAGE_INDEX, PROJECT_ACTIVITY_INDEX]
    private static final List<String> INDEX_SUFFIXES = ["_1", "_2"]

    /** The elasticsearch client */
    RestHighLevelClient client
    /** Any indexes created / aliased by this class will be prefixed with this value */
    String indexPrefix

    /** Settings to use when creating a new index @see https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#index-modules-settings */
    Map settings

    /** Static mapping to use when indexing data @see https://www.elastic.co/guide/en/elasticsearch/reference/current/explicit-mapping.html */
    Map mapping

    ElasticSearchIndexManager(RestHighLevelClient client, String indexPrefix, Map indexSettings, Map indexMapping) {
        this.client = client
        this.indexPrefix = indexPrefix
        this.settings = indexSettings
        this.mapping = indexMapping
    }

    /**
     * Intended to run on startup, this method checks that the elasticsearch indexes exist and
     * appropriate aliases are defined to use those indexes.
     * The main use case is when switching between development and testing modes, and also when
     * running for the first time against a new instance of elasticsearch.
     */
    void initialiseIndexAliases() {

        ALL_INDEX_TYPES.each { String alias ->
            indexNamesForAlias(alias).each { String indexName ->
                GetIndexRequest request = new GetIndexRequest(indexName)
                IndicesClient indicesClient = client.indices()
                boolean exists = indicesClient.exists(request, RequestOptions.DEFAULT);
                if (!exists) {
                    // Create the index
                    createIndexAndMapping(indexName)
                }
            }

        }
        ALL_INDEX_TYPES.each { String alias ->
            GetAliasesRequest aliasesRequest = new GetAliasesRequest(alias)
            GetAliasesResponse response = client.indices().getAlias(aliasesRequest, RequestOptions.DEFAULT)

            // We need to update the alias in dev/test as the prefix is different if we've been running tests
            if (!isAliasCorrect(alias, response)) {
                String indexName = indexNamesForAlias(alias)[0]
                updateAlias(alias, indexName)
            }
        }
    }

    /** Returns true if the alias is pointing to a supported index */
    private boolean isAliasCorrect(String alias, GetAliasesResponse response) {
        // If the alias doesn't exist or the alias points to more than one index, we need to update it
        if (response.status() != RestStatus.OK || response.aliases.size() > 1) {
            return false
        }
        String index = response.aliases.keySet().first()
        return index in indexNamesForAlias(alias)
    }

    /**
     * Each alias can point to one of two real indexes - this is done so we can reindex without impacting the search
     * results, then swap the alias to the  new index when re-indexing is complete.
     * @param alias the alias of interest.
     */
    List<String> indexNamesForAlias(String alias) {
        INDEX_SUFFIXES.collect {String suffix ->
            indexPrefix+"_"+alias+suffix
        }
    }

    /**
     * Updates an alias to point to the supplied index.  Used during initialisation or when swapping
     * primary indexes after a full re-index of the system.
     * @param alias the alias to update
     * @param index the index the alias should point to.
     * @return true if the update completed successfully.
     */
    boolean updateAlias(String alias, String index) {
        log.info("Creating alias "+alias+" for index "+index)

        // Remove any existing aliases with the same name
        GetAliasesRequest getAliasesRequest = new GetAliasesRequest(alias)
        GetAliasesResponse response = client.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT)
        List<String> indexNames = []
        if (response.status() == RestStatus.OK) {
            indexNames = response.aliases?.collect { String indexName, Set values -> indexName }
            if (indexNames.size() == 1 && indexNames[0] == index) {
                return true // The alias is already pointing to the correct index.
            }
        }

        // Add the names of the indexes to remove from the alias
        indexNames = indexNames.findAll { it != index }
        IndicesAliasesRequest request = new IndicesAliasesRequest();

        indexNames.each { String indexName ->
            IndicesAliasesRequest.AliasActions aliasAction =
                    new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                            .index(indexName)
                            .alias(alias)
            request.addAliasAction(aliasAction)
        }
        // Create the request to add the new index to the alias
        IndicesAliasesRequest.AliasActions aliasAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                        .index(index)
                        .alias(alias)
        request.addAliasAction(aliasAction)
        AcknowledgedResponse indicesAliasesResponse =
                client.indices().updateAliases(request, RequestOptions.DEFAULT)
        return indicesAliasesResponse.acknowledged
    }

    /**
     * Create a new index add configure custom mappings
     */
    private void createIndexAndMapping(String index) {
        log.info "Creating new index and configuring elastic search custom mapping"
        try {

            CreateIndexRequest request = new CreateIndexRequest(index)
            request.mapping(mapping)
            request.settings(settings)
            client.indices().create(request, RequestOptions.DEFAULT)

        } catch (Exception e) {
            log.error "Error creating index: ${e}", e
        }
    }

    /**
     * Finds the index currently unused by the specified alias (or all aliases if no alias is specified), then
     * deletes and recreates it.
     * This is done to allow us to re-index into a new index then swap the alias when the re-index is complete
     * @return the name of the index that was recreated.
     */
    Map<String, String> recreateUnusedIndexes() {
        List aliases = ALL_INDEX_TYPES
        Map<String, String> toDeleteAndCreate = [:]
        aliases.each { String alias ->
            log.info "trying to delete $alias"

            GetAliasesRequest request = new GetAliasesRequest(alias)
            GetAliasesResponse response = client.indices().getAlias(request, RequestOptions.DEFAULT)

            toDeleteAndCreate[alias] = indexNamesForAlias(alias).find { String indexName ->
                !response.aliases[indexName]
            }
            try {
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(toDeleteAndCreate[alias])
                AcknowledgedResponse deleteResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT)
                if (deleteResponse.acknowledged) {
                    log.info "The index is removed"
                } else {
                    log.error "The index could not be removed"
                }
            } catch (Exception e) {
                log.error "The index you want to delete is missing : ${e.message}"
            }
            createIndexAndMapping(toDeleteAndCreate[alias])
        }
        toDeleteAndCreate
    }

}
