package au.org.ala.ecodata

import au.org.ala.ecodata.config.StorageConfig
import au.org.ala.ecodata.graphql.config.GraphQLConfig
import grails.boot.GrailsApp
import grails.boot.config.GrailsApplicationPostProcessor
import grails.boot.config.GrailsAutoConfiguration
import grails.core.GrailsApplication
import grails.core.GrailsApplicationLifeCycle
import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext
import graphql.schema.GraphQLSchema
import graphql.schema.StaticDataFetcher
import graphql.schema.idl.*
import org.apache.grails.core.plugins.PluginDiscovery
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import


// Grails 8 no longer processes @Configuration classes declared in resources.groovy as configuration classes
// (their @Bean methods are ignored), so they are imported here instead.
@Import([GraphQLConfig, StorageConfig])
class Application extends GrailsAutoConfiguration {

    private static final String EHCACHE_DIRECTORY_CONFIG_ITEM = "ehcache.directory"
    private static final String DEFAULT_EHCACHE_DIRECTORY = "./ehcache"

    static {
        BsonDocumentConverterWorkaround.install()
    }

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
    GrailsApplicationPostProcessor grailsApplicationPostProcessor(PluginDiscovery pluginDiscovery) {

        // We are overriding the GrailsApplicationPostProcessor because we need a lifecycle hook after
        // the configuration has been read, but before the plugin lifecycle bean initialisation has started.
        // This is because the grails ehcache plugin only supports configuration via XML files and the
        // cache directory store can only be configured via an environment variable.
        // To keep the configuration in one place, we are reading the config, and setting the system property
        // so it can be read during cache initialisation.
        Class[] classesArray = classes().toArray(new Class[0])
        return new EcodataGrailsApplicationPostProcessor(this, applicationContext, pluginDiscovery, classesArray)
    }

    /**
     * Named (rather than anonymous) subclass so the varargs super constructor call is statically bound.
     * Groovy's generated constructor for an anonymous inner class mishandles the Class... varargs parameter,
     * resulting in a GroovyCastException (PojoWrapper -> Class).
     */
    @CompileStatic
    static class EcodataGrailsApplicationPostProcessor extends GrailsApplicationPostProcessor {
        EcodataGrailsApplicationPostProcessor(GrailsApplicationLifeCycle lifeCycle, ApplicationContext applicationContext,
                                              PluginDiscovery pluginDiscovery, Class[] classes) {
            super(lifeCycle, applicationContext, pluginDiscovery, classes)
        }

        @Override
        protected void customizeGrailsApplication(GrailsApplication grailsApplication) {
            System.setProperty(EHCACHE_DIRECTORY_CONFIG_ITEM, grailsApplication.config.getProperty(EHCACHE_DIRECTORY_CONFIG_ITEM, DEFAULT_EHCACHE_DIRECTORY))
        }
    }
}