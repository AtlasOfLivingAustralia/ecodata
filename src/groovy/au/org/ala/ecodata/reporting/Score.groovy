package au.org.ala.ecodata.reporting

/**
 * Defines metadata for an Output Score that informs the way the score is aggregated for reporting purposes.
 * TODO this information likely makes sense to be in the metadata so a this class definition may be temporary...
 */
class Score {

    /** Enumerates the currently supported ways to aggregate output scores. */
    enum AGGREGATION_TYPE {SUM, AVERAGE, COUNT, HISTOGRAM, SET}

    /** The name of the output to which the score belongs */
    String outputName

    /** The name of the score (as defined in the OutputModel */
    String name

    String category

    String listName

    String groupBy

    /** In the case that a groupBy term is specified, the filterBy term will select the value from a particular group */
    String filterBy

    /** "piechart" or "barchart" only currently */
    String displayType

    /** Defines how this score should be aggregated */
    AGGREGATION_TYPE aggregationType

    /** The label for this score when displayed */
    String label

    /** A more detailed description of the score and how it should be interpreted */
    String description

    /** Whether or not this score is suitable for use as a project output target */
    boolean isOutputTarget

    /** Used for mapping this score to the GMS */
    String gmsId

    /**
     * The units this score is measured in.  May not make sense for all scores or for an aggregrated result
     * (e.g. units don't make sense for a count based aggregation).
     */
    String units

    public defaultGrouping() {
        if (groupBy) {
            def bits = groupBy.split(':')
            if (bits.length == 2) {
                def property = ''
                if (bits[0] == 'output') {
                    property = "data."
                    if (listName) {
                        property+=listName+'.'
                    }
                    property += bits[1]
                }
                else {
                    property += bits[0]+'.'+bits[1]
                }
                def grouping = [property: property, groupTitle: label, type:'discrete']
                if (filterBy) {
                    grouping.filterBy = filterBy
                    grouping.type = 'filter'
                }
                return grouping

            }
        }
        return [entity: '*']
    }

    /**
     * Returns a List of Scores as defined for the supplied output metadata.
     * @param outputMetadata describes the output, including a description of the scores this output provides
     * @return a List<Score> of the Scores defined by the output metadata
     */
    static List<Score> outputScores(outputMetadata) {

        return outputMetadata.scores.collect{
            def score = new Score(it)
            score.outputName = outputMetadata.name
            score
        }

    }
}
