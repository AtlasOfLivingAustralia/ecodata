package au.org.ala.ecodata

/**
 * Helper class for creating test data.
 */
class TestDataHelper {

    static organisationCount = 1
    static Organisation buildOrganisation(Map props = [:]) {

        Map mandatoryProps = [organisationId:organisationCount] + props

        return buildNewOrganisation(mandatoryProps)
    }

    /** Builds an Organisation without a organisationId */
    static Organisation buildNewOrganisation(Map props = [:]) {

        Map mandatoryProps = [name:"Organisation ${organisationCount}"]

        // Allow properties to be overridden
        mandatoryProps.putAll(props)
        organisationCount++

        return new Organisation(mandatoryProps)
    }

    static projectCount = 1
    static Project buildProject(Map props = [:]) {

        Map mandatoryProps = [projectId:projectCount] + props

        return buildNewProject(mandatoryProps)
    }

    /** Builds a Project without a projectId */
    static Project buildNewProject(Map props = [:]) {

        Map mandatoryProps = [name:"Project ${projectCount}"]

        // Allow properties to be overridden
        mandatoryProps.putAll(props)
        projectCount++

        return new Project(mandatoryProps)
    }

    static void saveAll(List domainObjects) {
        domainObjects.each {
            it.save(flush:true)
        }
    }

}
