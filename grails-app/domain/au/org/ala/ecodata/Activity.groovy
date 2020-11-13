package au.org.ala.ecodata

import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.bson.types.ObjectId
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

/**
 * Currently this holds both activities and assessments.
 */
class Activity {

    /**
     * Values for activity progress.  An enum would probably be better but doesn't seem to
     * work out of the box with mongo/GORM
     */
    public static final String PLANNED = 'planned'
    public static final String STARTED = 'started'
    public static final String FINISHED = 'finished'
    public static final String DEFERRED = 'deferred'
    public static final String CANCELLED = 'cancelled'

    /*
    Note:
        activities and assessments are both described by this domain - 'activities' can be used to mean both
    Associations:
        activities must belong to 1 Site or 1 project - this is mapped by the siteId or projectId in this domain
        activities may have 0..n Outputs - these are mapped from the Output side
    */

    static graphql = GraphQLMapping.lazy {
        // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
        operations.get.enabled false
        operations.list.enabled true
        operations.count.enabled false
        operations.create.enabled false
        operations.update.enabled false
        operations.delete.enabled false

        add('outputs', [Output]) {
            dataFetcher { Activity activity ->
                Output.findAllByActivityId(activity.activityId)
            }
            input false
        }

        add('data', 'Data') {
            ActivityForm form = Holders.grailsApplication.mainContext.activityFormService.findActivityForm("RLP Output Report", 1)
            input false
            type {
                form.sections.each { FormSection section ->
                    if (section.template.dataModel) {

                        String typeName = section.name.replaceAll("[ |-]", "")
                        field(typeName, typeName) {

                            for (Map dataModelItem : section.template.dataModel) {
                                field(dataModelItem.name, String) {
                                    if (dataModelItem.description) {
                                        description(dataModelItem.description)
                                    }
                                }
                            }
                        }
                    }

                }
            }
            dataFetcher { Activity activity ->
                Map result = Output.findAllByActivityId(activity.activityId).collectEntries {
                    [(it.name.replaceAll("[ |-]", "")):it.data]
                }
                result
            }


        }
        query('activities', [Activity]) {
            argument('term', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    environment.context.grailsApplication.mainContext.activitiesFetcher.get(environment)
                }
            })
        }
    }


        static mapping = {
        activityId index: true
        siteId index: true
        projectId index: true
        projectActivityId index: true
        version false
    }

    ObjectId id
    String activityId
    String status = 'active'
    String progress = PLANNED
    Boolean assessment = false
    String siteId
    String projectId
    String projectActivityId
    String managementUnitId
    String description
    Date startDate
    Date endDate

    /** The type of activity performed.  This field must match the name of an ActivityForm */
    String type
    /**
     * The formVersion of the ActivityForm used to record the details of this activity. If not-null, the details of this activity should be
     * displayed using the formVersion here.
     */
    Integer formVersion

    /**
     * Allows grouping of project activities into stages or milestones for planning and reporting purposes.
     * Biodiversity & CFOC projects plan activities in six monthly groups (Stage 1, Stage 2...)
     */
    String projectStage
    Date plannedStartDate
    Date plannedEndDate
    /** The program sponsoring or funding this Activity (e.g. Biodiversity Fund Round 1) */
    String associatedProgram
    /** Allows for breakdown of a program into separately reportable units (e.g. Biodiversity Fund Round 1 has three themes) */
    String associatedSubProgram
    String collector
    String censusMethod
    String methodAccuracy
    String fieldNotes
    String notes
    Date dateCreated
    Date lastUpdated
    String userId
    Boolean embargoed

    /** An activity is considered complete if it's progress attribute is finished, deferred or cancelled. */
    public boolean isComplete() {
        def completedStates = [FINISHED, DEFERRED, CANCELLED]
        return progress in completedStates
    }

    /** Activities with a progress of DEFFERED or CANCELLED should not have any outputs associated with them */
    public boolean supportsOutputs() {
        return progress in [PLANNED, STARTED, FINISHED]
    }

    static transients = ['complete']

    static constraints = {
        siteId nullable: true
        projectId nullable: true
        projectActivityId nullable: true
        managementUnitId nullable: true
        description nullable: true
        startDate nullable: true
        endDate nullable: true
        type nullable: true
        collector nullable: true
        censusMethod nullable: true
        methodAccuracy nullable: true
        fieldNotes nullable: true, maxSize: 4000
        notes nullable: true, maxSize: 4000
        plannedStartDate nullable: true
        plannedEndDate nullable: true
        progress nullable: true
        associatedProgram nullable: true
        associatedSubProgram nullable: true
        projectStage nullable: true
        projectActivityId nullable: true
        userId nullable:true
        embargoed nullable:true
        formVersion nullable: true
    }

}
