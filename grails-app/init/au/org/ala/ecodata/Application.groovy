package au.org.ala.ecodata

import au.org.ala.userdetails.UserDetailsClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import grails.boot.GrailsApp
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

    @Autowired
    GrailsApplication grailsApplication

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

    @ConditionalOnMissingBean(name = "userDetailsHttpClient")
    @Bean(name = ["defaultUserDetailsHttpClient", "userDetailsHttpClient"])
    OkHttpClient userDetailsHttpClient() {
        Integer readTimeout = 3000
        return new OkHttpClient.Builder().readTimeout(readTimeout, MILLISECONDS).build()
    }

    @ConditionalOnMissingBean(name = "userDetailsMoshi")
    @Bean(name = ["defaultUserDetailsMoshi", "userDetailsMoshi"])
    Moshi userDetailsMoshi() {
        return new Moshi.Builder().add(Date, new Rfc3339DateJsonAdapter().nullSafe()).build()
    }

    @Bean("userDetailsClient")
    UserDetailsClient userDetailsClient(@Qualifier("userDetailsHttpClient") OkHttpClient userDetailsHttpClient,
                                        @Qualifier('userDetailsMoshi') Moshi userDetailsMoshi,
                                        GrailsApplication grailsApplication) {
        String baseUrl = grailsApplication.config.getProperty("userDetails.url")
        return new UserDetailsClient.Builder(userDetailsHttpClient, baseUrl).moshi(userDetailsMoshi).build()
    }
}