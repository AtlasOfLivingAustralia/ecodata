package au.org.ala.ecodata.metadata

/**
 * The program model defines a set of programs and sub-programs that can be used to group
 * projects and supply basic configuration information for those projects.
 */
class ProgramsModel {

    private Map model

    public ProgramsModel(Map model) {
        this.model = model
    }

    /**
     * Returns a List containing the activity types supported by a program or sub-program.  Activity
     * types can be specified at either the program or sub-program level.  Activities specified by
     * a sub-program will override any configuration at the program level.
     * @param programName the name of the program
     * @param subprogramName (optional) the name of the subprogram.
     * @return a List<String> containing the supported activity types
     */
    List<String> getSupportedActivityTypes(String programName, String subprogramName = null) {
        Map program = model.programs?.find{it.name == programName}
        List supportedActivityTypes = []
        if (subprogramName) {
            Map subprogram = program?.subprograms?.find{it.name == subprogramName}
            supportedActivityTypes = subprogram?.activities ?: []
        }
        if (!supportedActivityTypes) {
            supportedActivityTypes = program?.activities ?: []
        }
        supportedActivityTypes
    }
}
