package au.org.ala.ecodata

import javax.annotation.PostConstruct
import static java.lang.Math.*

class SensitiveSpeciesService {
    def sensitiveSpeciesData, webService, grailsApplication
    String googleMapsUrl, mapsApiKey
    static supportedZones = ['AUS', 'AU', 'NZ', 'VIC', 'WA', 'QLD', 'NT', 'ACT', 'TAS', 'NSW', 'SA']
    static earthRadiusInKm = 6378
    static transactional = false

    @PostConstruct
    void initialize() {
        log.info("Initializing species service")
        loadSensitiveData()
    }

    void loadSensitiveData() {
        log.info("Loading sensitive data.")
        googleMapsUrl = "${grailsApplication.config.google.maps.geocode.url}"
        mapsApiKey = "${grailsApplication.config.google.api.key}"
        try {
            File data = new File("${grailsApplication.config.sensitive.species.data}")
            if(data?.exists()){
                sensitiveSpeciesData = new XmlParser().parseText(data.getText('UTF-8'))
            } else {
                log.error("Sensitive species file (${grailsApplication.config.sensitive.species.data}) not found.")
            }
        } catch (Exception ex) {
            log.error("Error loading sensitive data xml file. ${ex}")
        }
    }

    /*
    * Check whether given species is in the sensitive list.
    * If found, alter the lat and lng value based on sensitive data generalisation value.
    *
    * @param speciesName
    * @param lat latitude
    * @param lng longitude
    * */
    Map findSpecies(String speciesName, double lat = 0.0, double lng = 0.0) {

        // Is species in the list
        def sensitiveSpecies = sensitiveSpeciesData?.'*'.find { sensitiveSpecies ->
            speciesName.equalsIgnoreCase(sensitiveSpecies.@name) || speciesName.equalsIgnoreCase(sensitiveSpecies.@commonName)
        }

        // Is generalisation applied to the species
        def instances = sensitiveSpecies?.instances?.'*'.findAll { instance ->
            instance.@generalisation && instance.@generalisation != ""
        }

        List spatialConfigs = []
        instances?.each { instance ->
            // Determine the zones
            Map item = [:]
            item.generalisation = instance.@generalisation
            item.zone = instance.@zone
            item.lat = 0.0
            item.lng = 0.0
            item.state = ""
            item.country = ""
            spatialConfigs << item
        }

        // Go through google maps api and determine whether given coordinates fall under specific zone.
        spatialConfigs?.each { item ->
            String latlng = "latlng=${lat},${lng}"
            String url = "${googleMapsUrl}?${latlng}&key=${mapsApiKey}"
            def geocode = webService.getJson(url)

            //Get Country and State Code
            if (geocode?.status == 'OK') {
                geocode.results?.each { entry ->
                    entry.address_components?.each { field ->
                        if (field.types?.find { it == 'administrative_area_level_1' }) {
                            item.state = field.short_name
                        } else if (field.types?.find { it == 'administrative_area_level_2' }) {
                            item.state = field.short_name
                        }

                        if (field.types?.find { it == 'country' }) {
                            item.country = field.short_name
                        }
                    }
                }
            }
        }

        // Now tweak the co-ordinates based on generalised value
        spatialConfigs?.each { item ->
            generaliseCoordinates(item, lat, lng)
        }

        spatialConfigs?.find {it.lat != 0.0 && it.lng != 0.0 } ?: [:]
    }

    private void generaliseCoordinates(Map item, double lat, double lng) {
        // Check species sensitive zone equals to geocode state or country
        if (code(item.zone) == code(item.state) || code(item.zone) == code(item.country)) {
            switch (item.generalisation) {
                case "10km":
                    item.lat = addDistanceToLat(lat, 10, 1)
                    item.lng = addDistanceToLng(lat, lng, 10, 1)
                    break
                case "1km":
                    item.lat = addDistanceToLat(lat, 1, 2)
                    item.lng = addDistanceToLng(lat, lng, 1, 2)
                    break
                case "100m":
                    item.lat = addDistanceToLat(lat, 0.1, 3)
                    item.lng = addDistanceToLng(lat, lng, 0.1, 3)
                    break
                default:
                    break
            }
        }
    }

    private String code(String code) {

        String zone = supportedZones?.find { code?.equalsIgnoreCase(it) }
        String standardCode
        switch (zone) {
            case 'AUS':
            case 'AU':
                standardCode = 'AUS'
                break;
            default:
                standardCode = zone
                break;
        }

        return standardCode?.toUpperCase()
    }

    private double addDistanceToLat(double lat, double distanceKm, int round){
        double newLat = lat
        if(distanceKm > 0) {
            newLat = adjustAccuracy(lat  + (distanceKm / earthRadiusInKm) * (180 / 3.14159), round)
        }
        newLat
    }

    private double addDistanceToLng(double lat, double lng, double distanceKm, int round){
        double newLng = lng
        if(distanceKm > 0) {
            newLng = adjustAccuracy(lng + (distanceKm / earthRadiusInKm) * (180 / 3.14159) / cos(lat * 3.14159 / 180), round)
        }

        newLng
    }

    private double adjustAccuracy (double coordinate, int round){
        switch (round) {
            case 3:
                coordinate = Math.round(coordinate * 1000) / 1000
                break
            case 2:
                coordinate = Math.round(coordinate * 100) / 100
                break
            case 1:
                coordinate = Math.round(coordinate * 10) / 10
                break
        }
        coordinate
    }

}
