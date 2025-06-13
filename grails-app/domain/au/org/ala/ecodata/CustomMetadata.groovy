package au.org.ala.ecodata

class CustomMetadata {
    String category

    Map<String, Object> toMap() {
        [
            nespCategory: category
        ]
    }
}
