package au.org.ala.ecodata.graphql.mappers

import au.org.ala.ecodata.Site
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class SiteGraphQLMapper {

    static graphqlMapping() {
        GraphQLMapping.lazy {
            // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
            operations.get.enabled false
            operations.list.enabled true
            operations.count.enabled false
            operations.create.enabled false
            operations.update.enabled false
            operations.delete.enabled false

            exclude 'extent', 'features', 'projects'

            add('siteGeojson', 'siteGeojson') {
                type {
                    field('type', String)
                    field('properties', 'properties') {
                        field('id', String)
                        field('name', String)
                        field('type', String)
                        field('notes', [String])
                    }
                    field('features', 'features') {
                        field('type', String)
                        field('geometry', 'featureGeometry'){
                            field('type', String)
                            field('coordinates', [String])
                        }
                        field('properties', 'featureProperties'){
                            field('id', String)
                        }
                        collection true
                    }
                    field("geometry", "geometry"){
                        field('type', String)
                        field('coordinates', [String])
                    }
                }
                dataFetcher { Site site, DataFetchingEnvironment environment ->
                    environment.context.grailsApplication.mainContext.sitesFetcher.getSitesAsGeojson(site.siteId)
                }
            }

            add('siteShapefiles', "siteShapefiles") {
                type {
                    field('url', String)
                }
                dataFetcher { Site site, DataFetchingEnvironment environment ->
                    def url = environment.context.grailsApplication.mainContext.sitesFetcher.getSiteShapeFileUrl(site.siteId)
                    return [url: url]
                }
            }

            query('sites', [Site]) {
                argument('term', String)
                argument('siteIds', [String]) { nullable true}
                argument('page', int){ nullable true }
                argument('max', int){ nullable true }
                dataFetcher(new DataFetcher() {
                    @Override
                    Object get(DataFetchingEnvironment environment) throws Exception {
                        environment.context.grailsApplication.mainContext.sitesFetcher.get(environment)
                    }
                })
            }
        }
    }
}
