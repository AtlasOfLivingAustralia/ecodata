package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.Site
import au.org.ala.ecodata.SiteService
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class GeoJsonConverter implements Coercing<Site, Map> {

    SiteService siteService

    GeoJsonConverter(SiteService siteService) {
        this.siteService = siteService
    }

    protected Optional<Map> convert(Object input) {
        if (input instanceof Site) {
            Map site = ((Site)input).properties
            Optional.of(siteService.toGeoJson(site))
        }
        else {
            Optional.empty()
        }
    }

    @Override
    Map serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a TargetMeasure")
        })
    }

    @Override
    Site parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a TargetMeasure")
        })
    }

    @Override
    Site parseLiteral(Object input) {
        null
    }
}
