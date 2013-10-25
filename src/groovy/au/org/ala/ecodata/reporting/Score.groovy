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

    String listName

    /** Defines how this score should be aggregated */
    AGGREGATION_TYPE aggregationType

    /** The label for this score when displayed */
    String label

    /** Whether or not this score is suitable for use as a project output target */
    boolean isOutputTarget

    /**
     * The units this score is measured in.  May not make sense for all scores or for an aggregrated result
     * (e.g. units don't make sense for a count based aggregation).
     */
    String units

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
