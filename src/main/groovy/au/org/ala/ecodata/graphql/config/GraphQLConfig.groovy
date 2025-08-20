package au.org.ala.ecodata.graphql.config

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.controller.GraphQLInterceptor
import au.org.ala.ecodata.graphql.converters.*
import au.org.ala.ecodata.graphql.models.TargetMeasure
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.EnumValuesProvider
import graphql.schema.idl.RuntimeWiring
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.graphql.server.WebGraphQlInterceptor
import reactor.core.publisher.Mono

@Configuration
class GraphQLConfig {

    @Autowired
    SiteService siteService
    @Autowired
    UserService userService
    @Autowired
    PermissionService permissionService
    @Autowired
    HubService hubService
    @Autowired
    MetadataService metadataService

    GraphQLConfig(BatchLoaderRegistry registry) {
        registerBatchLoaders(registry)
    }

    @Bean
    WebGraphQlInterceptor webGraphQlInterceptor() {
       new GraphQLInterceptor(userService, hubService, permissionService)
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
                 GraphQLScalarType.newScalar().name('TargetMeasure').description('Target Measure').coercing(new TargetMeasureConverter()).build(),
                 GraphQLScalarType.newScalar().name('AmountDelivered').description("Amount delivered").coercing(new MapConverter()).build(),
                 GraphQLScalarType.newScalar().name("GeoJson").description("GeoJSON object").coercing(new GeoJsonConverter(siteService)).build()

        ]
        GraphQLEnumType statusEnum = GraphQLEnumType.newEnum()
                .name("Status")
                .description("Project status")
                .value("ACTIVE", Status.ACTIVE, "Active project")
                .value("COMPLETED", Status.COMPLETED, "Inactive project")
                .value("APPLICATION", Status.APPLICATION, "Pending approval")
                .value("TERMINATED", Status.TERMINATED, "Project terminated")
                .value("DELETED", Status.DELETED, "Project deleted")
                .build();

        Map<String, String> statusEnumValues = [
                "ACTIVE":Status.ACTIVE,
                "COMPLETED":Status.COMPLETED,
                "APPLICATION":Status.APPLICATION,
                "TERMINATED":Status.TERMINATED,
                "DELETED":Status.DELETED
        ]
        Map<String, String> publicationStatusValues = [
                "DRAFT": PublicationStatus.DRAFT,
                "APPROVED": PublicationStatus.PUBLISHED,
                "SUBMITTED": PublicationStatus.SUBMITTED_FOR_REVIEW
        ]
        RuntimeWiringConfigurer configurer = new RuntimeWiringConfigurer() {
            @Override
            void configure(RuntimeWiring.Builder wiringBuilder) {
                scalarTypes.each { scalarType ->
                    wiringBuilder.scalar(scalarType)
                }
                registerEnumMapping(wiringBuilder, "Status", statusEnumValues)
                registerEnumMapping(wiringBuilder, "PublicationStatus", publicationStatusValues)
            }
        }
        configurer
    }

    private static void registerEnumMapping(RuntimeWiring.Builder wiringBuilder, String typeName, Map<String, String> values) {
        wiringBuilder.type(typeName, typeWiring ->
                typeWiring.enumValues(new EnumValuesProvider() {
                    @Override
                    Object getValue(String name) {
                        values[name]
                    }
                })
        )
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
        registry.forTypePair(String, Activity).registerMappedBatchLoader((activityIds, env) -> {
            Map activities = Activity.findAllByActivityIdInList(new ArrayList(activityIds)).collectEntries { Activity activity ->
                [(activity.activityId): activity]
            }
            Mono.just(activities)
        })

        registry.forTypePair(String, List<Output>).registerMappedBatchLoader((activityIds, env) -> {
            Map<String, List<Output>> outputs = Output.findAllByActivityIdInList(new ArrayList(activityIds)).groupBy { Output output -> output.activityId }

            Mono.just(outputs)
        })

        registry.forTypePair(String, AmountDelivered).registerMappedBatchLoader { (scoreIds, env) ->
            new AmountDelivered()
        }

        registry.forTypePair(String, TargetMeasure).registerMappedBatchLoader ( (scoreIds, env) -> {
            Map<String, TargetMeasure> targetMeasures = Score.findAllByScoreIdInList(new ArrayList(scoreIds)).collectEntries { Score score ->
                TargetMeasure targetMeasure = new TargetMeasure(
                        targetMeasureId: score.scoreId,
                        label: score.label,
                        name: score.name
                )
                targetMeasure.service = metadataService.serviceForScore(score.scoreId)
                [(score.scoreId): targetMeasure]
            }
            Mono.just(targetMeasures)
        })

        registry.forTypePair(Integer, Service).registerMappedBatchLoader ( (serviceIds, env) -> {
            Mono.just(Service.findAllByLegacyIdInList(new ArrayList(serviceIds)).collectEntries { Service service ->
                [(service.legacyId): service]
            })
        })
    }

}
