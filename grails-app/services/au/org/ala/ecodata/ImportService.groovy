package au.org.ala.ecodata

import au.com.bytecode.opencsv.CSVReader
import org.apache.commons.lang.StringUtils

/**
 * Handles data import into ecodata.
 */
class ImportService {

    static int INSTITUTION_DIFFERENCE_THRESHOLD = 4
    public static final List CSV_HEADERS = ['Grant ID', 'Grant Name', 'Grant Description', 'Grant Status', 'Grantee Full Name', 'Application Location Desc', 'Original Approved Amount', 'Round Name']

    /** The current format location data is supplied in */
    def locationRegExp = /lat. = ([\-0-9\.]*)\nlong. = ([\-0-9\.]*)\nLocation Description = (.*)lat\. =.*/

    def projectService, siteService, metadataService


    /**
     * Validates and imports project, site and institution data supplied by the GMS as a CSV file.
     */
    def importProjectsByCsv(InputStream csv) {
        CSVReader csvReader = new CSVReader(new InputStreamReader(csv));
        def headerIndexes = validateHeader(csvReader.readNext());

        if (headerIndexes.missing) {
            return [success:false, error:"Invalid CSV file - missing header fields: ${headerIndexes.missing}"]
        }

        def results = [success:false, validationErrors:[]]

        String[] csvLine = csvReader.readNext();
        while (csvLine) {
            def project = validateProjectDetails(mapProjectDetails(headerIndexes, csvLine))
            if (project.errors.size() > 0) {
                writeErrors(results.validationErrors, project.errors, csvLine)
            }
            csvLine = csvReader.readNext()
        }

        return results

    }

    def validateProjectDetails(projectDetails) {

        def project = [:]
        def errors = []
        project.errors = errors

        CSV_HEADERS.each {
            if (!projectDetails[it]) {
                errors.add("No value for '${it}'")
            }
        }

        if (projectDetails['Application Location Desc']) {
            def locationData = (projectDetails['Application Location Desc'] =~ locationRegExp)
            if (!locationData) {
                errors.add("'Application Location Desc' doesn't match the expected format")
            }
        }

        if (projectDetails['Round Name']) {
            def roundDetails = projectDetails['Round Name'];
            def program = metadataService.programsModel().programs.find{ roundDetails.startsWith(it.name)}

            if (!program) {
                errors.add("'Round Name' does not match a valid program name")
            }
            else {
                project.associatedProgram = program
                def subprogram = program.subprograms.find {roundDetails.contains(it.name)}
                if (!subprogram) {
                    errors.add("'Round Name' does not match a valid subprogram name")
                }
                else {
                    project.associatedSubProgram = subprogram
                }
            }

        }


        return project
    }

    def createProject(projectDetails) {
        // We are matching by description because it's either that or lat/lon at the moment...
        def site = Site.findByDescription(projectDetails.siteDescription)
        if (!site) {
            def potentialMatches = matchSiteDescription()
        }

        def institution = metadataService.getInstitutionByName(institutionName)
        if (!institution) {
            def potentialMatches = matchInstitution(institutionName)
        }


        def program = metadataService.programsModel().find {it.name == project.associatedProgram}
        if (program) {
            def subprogram = program.subprograms.find {it.name == associatedSubprogram}
        }
    }



    def matchInstitution(String institutionName) {

        def lowerCaseName = institutionName.toLowerCase()
        return metadataService.institutionList().findAll({StringUtils.getLevenshteinDistance(it.name.toLowerCase(), lowerCaseName) < INSTITUTION_DIFFERENCE_THRESHOLD})
    }


    private def writeErrors(results, errors, String[] line) {
        def errorLine = []
        errorLine << errors.join('\n')
        line.each{
            // The truncation is because the CSV file is slow because of the large amount of repetition in the
            // location cells in particular
            errorLine << it.substring(0, Math.min(it.length(), 40)).replaceAll('\n', ' ').replaceAll('"', "'")
        }

        results << errorLine
    }

    private def validateHeader(String[] headerTokens) {

        def suppliedHeaders = headerTokens as List
        def missingHeaders = []
        def headerIndicies = [:]
        CSV_HEADERS.each{
            int index = suppliedHeaders.indexOf(it)
            if (index < 0) {
                missingHeaders << it
            }
            headerIndicies << [(it):index]
        }
        if (missingHeaders.size() > 0) {
            headerIndicies.missing = missingHeaders
        }
        headerIndicies

    }

    private def mapProjectDetails(headerDetails, String[] csvLine) {

        def projectDetails = [:]
        headerDetails.each {key, value ->
            projectDetails << [(key):csvLine[value]]

        }
        projectDetails
    }



}
