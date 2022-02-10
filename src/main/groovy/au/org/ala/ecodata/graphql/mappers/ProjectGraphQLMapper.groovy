package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.Activity
import au.org.ala.ecodata.Document
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.ProjectActivity
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.Site
import au.org.ala.ecodata.Status
import au.org.ala.ecodata.graphql.enums.DateRange
import au.org.ala.ecodata.graphql.enums.ProjectStatus
import au.org.ala.ecodata.graphql.enums.YesNo
import au.org.ala.ecodata.graphql.fetchers.ActivityFetcher
import au.org.ala.ecodata.graphql.fetchers.Helper
import au.org.ala.ecodata.graphql.fetchers.ProjectsFetcher
import au.org.ala.ecodata.graphql.models.MeriPlan
import au.org.ala.ecodata.graphql.models.OutputData
import grails.gorm.DetachedCriteria
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.ClosureDataFetchingEnvironment
import org.grails.gorm.graphql.fetcher.impl.SingleEntityDataFetcher
import org.apache.commons.lang.WordUtils

class ProjectGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled true
            operations.list.paginate(true)
            operations.count.enabled false
            operations.create.enabled true
            operations.update.enabled false
            operations.delete.enabled false

            exclude("custom")

            Map activityModel = [:] //new Helper().getActivityOutputModels()
            String[] duplicateOutputs = [] //activityModel["activities"].outputs.name.flatten().groupBy { it }.findAll { it.value.size() > 1}.keySet()

            List<String> restrictedProperties = []
            restrictedProperties.each { String prop ->
                property(prop) {
                    dataFetcher { Project project, ClosureDataFetchingEnvironment env ->
                        boolean canRead = env.environment.context.acl.canRead(env.source, project)
                        if (canRead) {
                            return project[prop]
                        }
                        return null
                    }
                }
            }

            add('meriPlan', MeriPlan) {
                dataFetcher { Project project ->
                    project.getMeriPlan()
                }
            }

            add('surveys', [ProjectActivity]) {
                dataFetcher { Project project, ClosureDataFetchingEnvironment env ->
                    ProjectActivity.findAllByProjectId(project.projectId)
                }
            }


            add('documents', [Document]) {
                dataFetcher { Project project, ClosureDataFetchingEnvironment env ->
                    Document.findAllByProjectIdAndStatusNotEqual(project.projectId, Status.DELETED)
                }
            }
            add('reports', [Report]) {
                dataFetcher { Project project ->
                    Report.findAllByProjectIdAndStatusNotEqual(project.projectId, Status.DELETED)
                }
            }

            add('sites', [Site]) {
                dataFetcher { Project project ->
                    Site.findAllByProjectsAndStatusNotEqual(project.projectId, Status.DELETED)
                }
            }

            add('activities', [Activity]) {
                dataFetcher { Project project ->
                    new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                            Holders.applicationContext.messageSource, Holders.grailsApplication).getFilteredActivities(project.tempArgs, project.projectId)
                }
            }

            //add graphql type for each activity type
            activityModel["activities"].each {
                if(it.name && it.outputs && it.outputs.size() > 0 && it.outputs.fields?.findAll{ x -> x?.size() != 0 }?.size() > 0){
                    def outputTypes = it.outputs
                    String activityName = (it.name).replaceAll("\\W", "")
                    String name = "Activity_" + activityName
                    List outputList = []
                    List modifiedColumns = []
                    String desc = it.name
                    //define activity type
                    add(name, name) {
                        type {
                            outputTypes.each { outputType ->
                                String outputName = (outputType.name).replaceAll("\\W", "")
                                String title = outputType.title
                                String outputTypeName = "OutputType_" + outputName
                                if(outputType.fields?.size() > 0 && !(outputTypeName in outputList)) {
                                    if(duplicateOutputs.contains(outputType.name)){
                                        outputTypeName = "OutputType_" + activityName + "_" + outputName
                                    }
                                    outputList << outputTypeName
                                    //define output types and fields of the activity
                                    field(outputTypeName, outputTypeName) {
                                        String[] fieldList = outputType.fields.name.unique()
                                        for(int t=0; t<fieldList.size(); t++){
                                            def outputField = outputType.fields.find{ b -> b.name == fieldList[t]}
                                            if(outputField.dataType == "number") {
                                                field(fieldList[t].toString(), double) {description outputField.label}
                                            }
                                            else if(outputField.dataType == "text") {
                                                field(fieldList[t].toString(), String) {description outputField.label}
                                            }
                                            else if(outputField.dataType == "list") {
                                                modifiedColumns << fieldList[t].toString()
                                                field(outputTypeName + "_" + fieldList[t].toString(), outputTypeName + "_" + fieldList[t].toString()){
                                                    String[] columnList = outputField.columns.name.unique()
                                                    for(int y=0; y<columnList.size(); y++){
                                                        def column = outputField.columns.find{ b -> b.name == columnList[y]}
                                                        field(column.name.toString(), column.dataType == "number" ? double : String) {description column.description}
                                                    }
                                                    collection true
                                                    description outputField.label
                                                }
                                            }
                                            else {
                                                field(fieldList[t].toString(), String) {description outputField.label}
                                            }
                                        }
                                        nullable true
                                        collection true
                                        description title
                                    }
                                }
                            }
                        }
                        description desc
                        dataFetcher { Project project ->
                            return new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                                    Holders.applicationContext.messageSource, Holders.grailsApplication).getActivityData(project.tempArgs, project.projectId, activityName, outputList, modifiedColumns)
                        }
                    }
                }
            }

            // get project by ID
            query('project', Project) {
                argument('projectId', String)
                dataFetcher(new SingleEntityDataFetcher<Project>(Project.gormPersistentEntity) {
                    @Override
                    protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                        Project.where { projectId == environment.getArgument('projectId') }
                    }
                })
            }

            query('projects', [Project]) {
                argument('term', String)
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().get(environment)
                    }
                })
            }

            query('searchMeritProject', [Project]) {
                argument('projectId', String) { nullable true }
                argument('fromDate', String){ nullable true description "yyyy-mm-dd"  }
                argument('toDate', String){ nullable true description "yyyy-mm-dd" }
                argument('dateRange', DateRange){ nullable true }
                argument('status', [String]){ nullable true }
                argument('organisation', [String]){ nullable true }
                argument('associatedProgram', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('mainTheme', [String]){ nullable true }
                argument('state', [String]){ nullable true }
                argument('lga', [String]){ nullable true }
                argument('cmz', [String]){ nullable true }
                argument('partnerOrganisationType', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('primaryOutcome', [String]){ nullable true }
                argument('secondaryOutcomes', [String]){ nullable true }
                argument('tags', [String]){ nullable true }

                argument('managementArea', [String]){ nullable true }
                argument('majorVegetationGroup', [String]){ nullable true  }
                argument('biogeographicRegion', [String]){ nullable true }
                argument('marineRegion', [String]){ nullable true }
                argument('otherRegion', [String]){ nullable true }
                argument('grantManagerNominatedProject', [YesNo]){ nullable true }
                argument('federalElectorate', [String]){ nullable true }
                argument('assetsAddressed', [String]){ nullable true }
                argument('userNominatedProject', [String]){ nullable true }
                argument('managementUnit', [String]){ nullable true }

                argument('page', int){ nullable true }
                argument('max', int){ nullable true }
                argument('myProjects', Boolean){ nullable true }

                //activities filter
                argument('activities', 'activities') {
                    accepts {
                        field('activityType', String) {nullable true}
                        field('output', 'output') {
                            field('outputType', String) {nullable false}
                            nullable true
                            //one activity can have zero or more output
                            collection true
                        }
                        //one project can have many activities
                        collection true
                    }
                    nullable true
                }

                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().searchMeritProject(environment)
                    }
                })
            }

            query('activityOutput', "activityOutput") {
                argument('fromDate', String){ nullable true description "yyyy-mm-dd"  }
                argument('toDate', String){ nullable true description "yyyy-mm-dd" }
                argument('dateRange', DateRange){ nullable true }
                argument('status', [String]){ nullable true }
                argument('organisation', [String]){ nullable true }
                argument('associatedProgram', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('mainTheme', [String]){ nullable true }
                argument('state', [String]){ nullable true }
                argument('lga', [String]){ nullable true }
                argument('cmz', [String]){ nullable true }
                argument('partnerOrganisationType', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('primaryOutcome', [String]){ nullable true }
                argument('secondaryOutcomes', [String]){ nullable true }
                argument('tags', [String]){ nullable true }

                argument('managementArea', [String]){ nullable true }
                argument('majorVegetationGroup', [String]){ nullable true  }
                argument('biogeographicRegion', [String]){ nullable true }
                argument('marineRegion', [String]){ nullable true }
                argument('otherRegion', [String]){ nullable true }
                argument('grantManagerNominatedProject', [YesNo]){ nullable true }
                argument('federalElectorate', [String]){ nullable true }
                argument('assetsAddressed', [String]){ nullable true }
                argument('userNominatedProject', [String]){ nullable true }
                argument('managementUnit', [String]){ nullable true }

                argument('activityOutputs', 'activityOutputs') {
                    accepts {
                        field('category', String) {nullable false}
                        field('outputs', 'outputList') {
                            field('outputType', String) {nullable false}
                            field('labels', [String]) {nullable true}
                            nullable true
                            collection true
                        }
                        collection true
                    }
                    nullable true
                }

                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().searchActivityOutput(environment)
                    }
                })
                returns {
                    field('outputData', 'outputData') {
                        field('category', String)
                        field('outputType', String)
                        field('result', 'result') {
                            field('label', String)
                            field('result', double) {nullable true}
                            field('resultList', OutputData) {nullable true}
                            field('groups', 'groups') {
                                field('group', String)
                                field('results', 'results') {
                                    field('count', int)
                                    field('result', double)
                                    collection true
                                }
                                nullable true
                                collection true
                            }
                        }
                        collection true
                    }
                }
            }

            query('outputTargetsByProgram', "outputTargetsByProgram") {
                argument('fromDate', String){ nullable true description "yyyy-mm-dd"  }
                argument('toDate', String){ nullable true description "yyyy-mm-dd" }
                argument('dateRange', DateRange){ nullable true }
                argument('status', [String]){ nullable true }
                argument('organisation', [String]){ nullable true }
                argument('associatedProgram', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('mainTheme', [String]){ nullable true }
                argument('state', [String]){ nullable true }
                argument('lga', [String]){ nullable true }
                argument('cmz', [String]){ nullable true }
                argument('partnerOrganisationType', [String]){ nullable true  }
                argument('associatedSubProgram', [String]){ nullable true }
                argument('primaryOutcome', [String]){ nullable true }
                argument('secondaryOutcomes', [String]){ nullable true }
                argument('tags', [String]){ nullable true }

                argument('managementArea', [String]){ nullable true }
                argument('majorVegetationGroup', [String]){ nullable true  }
                argument('biogeographicRegion', [String]){ nullable true }
                argument('marineRegion', [String]){ nullable true }
                argument('otherRegion', [String]){ nullable true }
                argument('grantManagerNominatedProject', [YesNo]){ nullable true }
                argument('federalElectorate', [String]){ nullable true }
                argument('assetsAddressed', [String]){ nullable true }
                argument('userNominatedProject', [String]){ nullable true }
                argument('managementUnit', [String]){ nullable true }

                argument('programs', [String]) {nullable true}
                argument('outputTargetMeasures', [String]) {nullable true}

                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().searchOutputTargetsByProgram(environment)
                    }
                })
                returns {
                    field('targets', 'targets') {
                        field('program', String)
                        field('outputTargetMeasure', 'outputTargetMeasure') {
                            field('outputTarget', String)
                            field('count', int)
                            field('total', double)
                            collection true
                        }
                        nullable true
                        collection true
                    }
                }
            }

            query('searchBioCollectProject', [Project]) {
                argument('projectId', String) { nullable true }
                argument('hub', String) { nullable false }
                argument('isWorldWide', Boolean){ nullable true }
                argument('projectStartFromDate', String){ nullable true description "yyyy-mm-dd"  }
                argument('projectStartToDate', String){ nullable true description "yyyy-mm-dd" }
                argument('scienceType', [String]){ nullable true }
                argument('tags', [String]){ nullable true }
                argument('countries', [String]){ nullable true }
                argument('ecoScienceType', [String]){ nullable true }
                argument('status', [ProjectStatus]){ nullable true }
                argument('difficulty', [String]){ nullable true }
                argument('organisation', [String]){ nullable true }
                argument('origin', [String]){ nullable true }
                argument('isBushfire', [Boolean]){ nullable true }
                argument('associatedProgram', [String]){ nullable true }
                argument('typeOfProject', [String]){ nullable true }
                argument('lga', [String]){ nullable true }

                argument('page', int){ nullable true }
                argument('max', int){ nullable true }
                argument('myProjects', Boolean){ nullable true }

                //activities filter
                argument('activities', 'activitiesList') {
                    accepts {
                        field('activityType', String) {nullable true}
                        field('output', 'outputsList') {
                            field('outputType', String) {nullable false}
                            nullable true
                            //one activity can have zero or more output
                            collection true
                        }
                        //one project can have many activities
                        collection true
                    }
                    nullable true
                }

                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        ProjectGraphQLMapper.buildTestFetcher().searchBioCollectProject(environment)
                    }
                })
            }

        }
    }


    static ProjectsFetcher buildTestFetcher() {

        new ProjectsFetcher(Holders.applicationContext.projectService, Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.reportService,
        Holders.applicationContext.cacheService, Holders.applicationContext.hubService)

    }
}
