package au.org.ala.ecodata

class CustomMetadata {
    String raid
    String category
    String nationalScale

    Map<String, Object> toMap() {
        [
            nespRaid: raid,
            nespCategory: category,
            nespNationalScale: nationalScale
        ]
    }
}
