package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.ProgramGraphQLMapper
import au.org.ala.ecodata.graphql.mappers.ProjectGraphQLMapper
import org.bson.types.ObjectId
import org.springframework.validation.Errors

/**
 * A program acts as a container for projects, more or less.
 */
class Program {

    static graphql = ProgramGraphQLMapper.graphqlMapping()

    ObjectId id
    String programId
    /** The hubId of the hub in which this Program was created */
    String hubId
    String name
    String acronym
    String description
    String status = Status.ACTIVE
    String url
    Date dateCreated
    Date lastUpdated

    List risks
    /** Themes for this program */
    List themes
    /** Assets managed by this program (e.g. threatened species, or ecological communities) */
    List assets
    /** Outcomes to be achieved by this program */
    List outcomes
    /** (optional) The siteId of a Site that defines the geographic area targeted or managed by this Program */
    String programSiteId

    /** Allows program administrators to publicise and communicate about the program */
    List blog

    /** Priorities for program outcomes */
    List priorities

    /** Configuration related to the program */
    Map config

    Date startDate
    Date endDate

    List<Program> subPrograms = []
    Program parent

    List<AssociatedOrg> associatedOrganisations

    /** Grant/procurement etc */
    String fundingType

    /** The total funding for this program */
    BigDecimal totalFunding

    /** Records the id/s of this program has held in external systems */
    List<ExternalId> externalIds

    /** Custom rendering for the program */
    Map toMap() {
        Map program = [:]
        program.programId = programId
        program.name = name
        program.description = description
        program.startDate = startDate
        program.endDate = endDate
        program.dateCreated = dateCreated
        program.lastUpdated = lastUpdated
        program.url = url
        program.programSiteId = programSiteId
        program.themes = themes
        program.assets = assets
        program.outcomes = outcomes
        program.priorities = priorities
        program.inheritedConfig = getInheritedConfig()
        program.config = config
        program.risks = risks
        program.parent = populateParentProgramSummary(parent)
        program.subPrograms = subPrograms
        program.blog = blog
        program.acronym = acronym
        program.fundingType = fundingType
        program.externalIds = externalIds
        program.totalFunding = totalFunding

        program.associatedOrganisations = associatedOrganisations

        program
    }

    /**
     * Walks up the tree of parents for this Program and produces a summary of the parent names and programIds.
     * @param currentProgram the program currently being summarized.
     * @return a Map containing [name:, programId:, parent:]
     */
    private Map populateParentProgramSummary(Program currentProgram) {
        if (!currentProgram) {
            return null
        }
        Map programSummary = [
                name:currentProgram.name,
                programId:currentProgram.programId
        ]
        if (currentProgram.parent) {
            programSummary.parent = populateParentProgramSummary(currentProgram.parent)
        }
        return programSummary

    }

    Map getInheritedConfig() {
        Program program = this
        Deque<Map> allConfig = new LinkedList<Map>()
        while(program != null) {
            if (program.config) {
                allConfig.push(program.config)
            }
            program = program.parent
        }

        Map result = [:]

        Map config = allConfig.peek()
        while (config != null) {
            result.putAll(allConfig.pop())
            config = allConfig.peek()
        }

        result
    }

    static mapping = {
        programId index: true
        version false
    }

    static embedded = ['associatedOrganisations', 'externalIds']

    static hasMany = [subPrograms:Program]

    static constraints = {
        name unique: true
        description nullable: true
        risks nullable: true
        subPrograms nullable: true
        startDate nullable: true
        endDate nullable: true
        url nullable: true
        config nullable: true
        parent nullable: true
        associatedOrganisations nullable:true
        programSiteId nullable: true
        acronym nullable: true
        fundingType nullable: true
        externalIds nullable: true
        totalFunding nullable: true
        hubId nullable: true, validator: { String hubId, Program program, Errors errors ->
            GormMongoUtil.validateWriteOnceProperty(program, 'programId', 'hubId', errors)
        }
    }

    public String toString() {
        return "Name: "+name+ ", description: "+description
    }
}
