import au.org.ala.ecodata.ManagementUnit
import au.org.ala.ecodata.Organisation
import au.org.ala.ecodata.Program
import au.org.ala.ecodata.graphql.converters.*
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactor.core.publisher.Mono

@Configuration
@Component
class GraphQLConfig implements WebMvcConfigurer {

    GraphQLConfig(BatchLoaderRegistry registry) {
        registerBatchLoaders(registry)
    }


    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer() {

        final List<GraphQLScalarType> scalarTypes = [
                GraphQLScalarType.newScalar().name("ObjectId").description("Hex representation of a Mongo object id").coercing(new ObjectIdConverter()).build(),
                 GraphQLScalarType.newScalar().name("object").description("").coercing(new ObjectConverter()).build(),
                 GraphQLScalarType.newScalar().name("OutputData").description("").coercing(new OutputDataConverter()).build(),
                 GraphQLScalarType.newScalar().name("SectionTemplate").description("").coercing( new SectionTemplateConverter()).build(),
                 GraphQLScalarType.newScalar().name("Summary").description("").coercing(new SummaryConverter()).build(),
                 GraphQLScalarType.newScalar().name("Schema").description("").coercing(new SchemaConverter()).build(),
                 GraphQLScalarType.newScalar().name("Date").description("").coercing(new DateFormatting()).build(),
                 GraphQLScalarType.newScalar().name('TargetMeasure').description('Target Measure').coercing(new TargetMeasureConverter()).build()
        ]

        RuntimeWiringConfigurer configurer = new RuntimeWiringConfigurer() {
            @Override
            void configure(RuntimeWiring.Builder wiringBuilder) {
                scalarTypes.each { scalarType ->
                    wiringBuilder.scalar(scalarType)
                }
            }
        }
        configurer
    }

    void registerBatchLoaders(BatchLoaderRegistry registry) {

        registry.forTypePair(String, Program).registerMappedBatchLoader((programIds, env) -> {
            Map programs = Program.findAllByProgramIdInList(new ArrayList(programIds)).collectEntries { Program program ->
                [(program.programId): program]
            }
            Mono.just(programs)
        })
        registry.forTypePair(String, ManagementUnit).registerMappedBatchLoader((managementUnitIds, env) -> {
            Map managementUnits = ManagementUnit.findAllByManagementUnitIdInList(new ArrayList(managementUnitIds)).collectEntries { ManagementUnit mu ->
                [(mu.managementUnitId): mu]
            }
            Mono.just(managementUnits)
        })
        registry.forTypePair(String, Organisation).registerMappedBatchLoader((organisationIds, env) -> {
            Map organisations = Organisation.findAllByOrganisationIdInList(new ArrayList(organisationIds)).collectEntries { Organisation organisation ->
                [(organisation.organisationId): organisation]
            }
            Mono.just(organisations)
        })
    }

}
