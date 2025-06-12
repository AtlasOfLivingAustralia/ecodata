package au.org.ala.ecodata

class NespMetadata implements ProjectMetadata {

    String raid
    String category
    String nationalScale

    @Override
    Map<String, Object> toMap() {
        return [
            nespRaid: raid,
            nespCategory: category,
            nespNationalScale: nationalScale
        ]
    }
}
