package au.org.ala.ecodata.graphql.config

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.controller.GraphQLInterceptor
import au.org.ala.ecodata.graphql.converters.*
import au.org.ala.ecodata.graphql.models.TargetMeasure
import grails.config.Config
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.tracing.TracingInstrumentation
import graphql.scalars.ExtendedScalars
import graphql.schema.*
import graphql.schema.idl.EnumValuesProvider
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer
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

    @Bean()
    @ConditionalOnProperty(value = "ecodata.graphql.enableTracingInstrumentation", havingValue = "true", matchIfMissing = false)
    Instrumentation instrumentation() {
        return new TracingInstrumentation()
    }

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer() {

        final List<GraphQLScalarType> scalarTypes = [
             GraphQLScalarType.newScalar().name("Date").description("Date without time").coercing(new DateFormatting()).build(),
             GraphQLScalarType.newScalar().name("DateTime").description("Date and time").coercing(new DateTimeFormatting()).build(),
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
                "SUBMITTED": PublicationStatus.SUBMITTED_FOR_REVIEW,
                "CANCELLED": PublicationStatus.CANCELLED
        ]

        Map<String, String> sitePurposeCodeEnumValues = [
                "PLANNING": Site.PLANNING_SITE_CODE,
                "REPORTING": Site.REPORTING_SITE_CODE,
                "EMSA": Site.EMSA_SITE_CODE]


        Map<String, String> progressEnumValues = [
                "PLANNED": Activity.PLANNED,
                "STARTED": Activity.STARTED,
                "FINISHED": Activity.FINISHED,
                "DEFERRED": Activity.DEFERRED,
                "CANCELLED": Activity.CANCELLED
        ]

        RuntimeWiringConfigurer configurer = new RuntimeWiringConfigurer() {
            @Override
            void configure(RuntimeWiring.Builder wiringBuilder) {
                scalarTypes.each { scalarType ->
                    wiringBuilder.scalar(scalarType)
                }
                wiringBuilder.scalar(ExtendedScalars.UUID)
                wiringBuilder.scalar(ExtendedScalars.Json)


                registerEnumMapping(wiringBuilder, "SitePurposeCode", sitePurposeCodeEnumValues)
                registerEnumMapping(wiringBuilder, "Status", statusEnumValues)
                registerEnumMapping(wiringBuilder, "PublicationStatus", publicationStatusValues)
                registerEnumMapping(wiringBuilder, "Progress", progressEnumValues)
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

        registry.forTypePair(String, InvestmentPriority).withName("assets").registerMappedBatchLoader((investmentPriorityIds, env) -> {
            Mono.just(InvestmentPriority.findAllByInvestmentPriorityIdInListAndStatusNotEqual(new ArrayList(investmentPriorityIds), Status.DELETED).collectEntries {[(it.investmentPriorityId):it] })
        })
    }

    /** Here we transform the schema to add descriptions from the DataDescription collection. */
    @Bean
    GraphQlSourceBuilderCustomizer sourceBuilderCustomizer() {
        return (builder) ->
                builder.schemaFactory { TypeDefinitionRegistry typeDefinitionRegistry, RuntimeWiring runtimeWiring ->
                    GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
                    //def enhancedSchema = new GraphQLSchemaDescriptionEnhancer().enhanceSchemaWithDescriptions(schema)
                    SchemaTransformer.transformSchema(schema, new GraphQLTypeVisitorStub() {
                        @Override
                        TraversalControl visitGraphQLObjectType(GraphQLObjectType node, TraverserContext<GraphQLSchemaElement> context) {
                            List<DataDescription> descriptions = DataDescription.findAllByEntity('au.org.ala.ecodata.'+node.name)
                            if (descriptions) {

                                changeNode(context, node.transform({ GraphQLObjectType.Builder objectBuilder ->
                                    node.fieldDefinitions.each { GraphQLFieldDefinition field ->
                                        DataDescription description = descriptions.find{it.graphQlName == field.name }
                                        if (description) {
                                            objectBuilder.field(field.transform {it.description(description.graphQLDescription?:description.description)})
                                        }

                                    }
                                }) )
                            }
                            return super.visitGraphQLObjectType(node, context)
                        }
                    })
                }

    }

}
