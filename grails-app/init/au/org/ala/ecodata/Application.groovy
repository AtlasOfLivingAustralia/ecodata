package au.org.ala.ecodata

import au.org.ala.userdetails.UserDetailsClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import grails.boot.GrailsApp
import grails.boot.config.GrailsApplicationPostProcessor
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import graphql.Scalars
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeRuntimeWiring
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

import static java.util.concurrent.TimeUnit.MILLISECONDS

//import groovy.util.logging.Slf4j
import org.springframework.context.annotation.ComponentScan

//@ComponentScan(basePackageClasses = EnvironmentDumper)
//@Slf4j
class Application extends GrailsAutoConfiguration {

    private static final String EHCACHE_DIRECTORY_CONFIG_ITEM = "ehcache.directory"
    private static final String DEFAULT_EHCACHE_DIRECTORY = "./ehcache"

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    GraphQLSchema schema() {
        String schema = "type Query{hello: String}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("Query").dataFetcher("hello", new StaticDataFetcher("world")) )
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }

    @Bean
    GrailsApplicationPostProcessor grailsApplicationPostProcessor() {

        // We are overriding the GrailsApplicationPostProcessor because we need a lifecycle hook after
        // the configuration has been read, but before the plugin lifecycle bean initialisation has started.
        // This is because the grails ehcache plugin only supports configuration via XML files and the
        // cache directory store can only be configured via an environment variable.
        // To keep the configuration in one place, we are reading the config, and setting the system property
        // so it can be read during cache initialisation.
        return new GrailsApplicationPostProcessor( this, applicationContext, classes() as Class[]) {
            @Override
            protected void customizeGrailsApplication(GrailsApplication grailsApplication) {
                System.setProperty(EHCACHE_DIRECTORY_CONFIG_ITEM, grailsApplication.config.getProperty(EHCACHE_DIRECTORY_CONFIG_ITEM, DEFAULT_EHCACHE_DIRECTORY))
            }

        }
    }
}