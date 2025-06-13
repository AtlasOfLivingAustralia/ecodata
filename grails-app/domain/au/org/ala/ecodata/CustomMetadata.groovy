package au.org.ala.ecodata

class CustomMetadata {

    String category

    @Override
    Map<String, Object> toMap() {
        return [
            nespCategory: category
        ]
    }
}
