package au.org.ala.ecodata

import static grails.async.Promises.task

class SpeciesReMatchService {
    static transactional = false
    def recordService, projectActivityService, webService, grailsApplication
    CacheService cacheService

    /*
    * Re Match GUID based on  scientific name or common name or name.
    *
    * If found update record collection with the new GUID.
    *
    * GORM event will trigger on each record update.
    *
    * Audit log will track the update.
    *
    * */

    def rematch() {

        task {
            Record.withSession { session -> session.clear() }
            Record.withNewSession {
                int max = 200
                int total = recordService.countRecords()
                int offset = 0, index = 0, nomatch = 0, error = 0

                while (offset < total) {
                    def records = recordService.getAll(max, offset)?.list
                    offset = offset + max

                    records?.each { record ->
                        def name = record.scientificName
                        if (!name) {
                            name = record.commonName
                        }
                        if (!name) {
                            name = record.name
                        }

                        def results = searchBie(name)
                        results?.autoCompleteList?.removeAll { !it.name }
                        results?.autoCompleteList?.each { result ->
                            result.scientificName = result.name
                            if (result.commonName && result.commonName.contains(',')) {
                                // ?. doesn't use groovy truth so throws exception for JSON.NULL
                                result.commonName = result.commonName.split(',')[0]
                            }

                            def recordResult = recordService.updateGuid(record.occurrenceID, result.guid)
                            if (recordResult.error) {
                                log.error("Error updating ${recordResult.error}")
                                error++
                            } else {
                                index++
                            }
                        }

                        if (results?.autoCompleteList.size() == 0) {
                            nomatch++
                        }
                    }

                    if((index + nomatch + error) % max == 0) {
                        log.info("Progress status: total records: ${total}")
                        log.info("Progress status: total records updated: ${index}")
                        log.info("Progress status: total species not found ${nomatch}")
                        log.info("Progress status: total db update error ${error}")
                        log.info("Progress status: offset: ${offset}")
                    }
                }

                log.info("Completed - Total records: ${total}")
                log.info("Completed - Total records updated: ${index}")
                log.info("Completed - Total species not found ${nomatch}")
                log.info("Completed - Total db update error ${error}")
                log.info("Completed - Offset: ${offset}")
            }
        }
    }

    def searchBie(String name, int limit = 1) {
        name = name?.toLowerCase() ?: ""
        cacheService.get('bie-search-auto-' + name, {
            def encodedQuery = URLEncoder.encode(name ?: '', "UTF-8")
            def url = "${grailsApplication.config.getProperty('bie.ws.url')}ws/search/auto.jsonp?q=${encodedQuery}&limit=${limit}&idxType=TAXON"

            webService.getJson(url)
        })
    }

    Map searchByName (String name, boolean addDetails = false, boolean useVernacularSearch = false ) {
        Map result
        if (!useVernacularSearch)
            result = searchNameMatchingServer(name)
        else
            result = searchByVernacularNameOnNameMatchingServer(name)
        List strategy = grailsApplication.config.getProperty('namematching.strategy', List)
        if (strategy.contains(result?.matchType)) {
            Map resp = [
                    scientificName: result.scientificName,
                    commonName: result.vernacularName,
                    guid: result.taxonConceptID,
                    taxonRank: result.rank
            ]

            if(addDetails) {
                resp.put('details', result)
            }

            return resp
        }
    }

    Map searchNameMatchingServer(String name) {
        name = name?.toLowerCase() ?: ""
        cacheService.get('name-matching-server-' + name, {
            def encodedQuery = URLEncoder.encode(name ?: '', "UTF-8")
            def url = "${grailsApplication.config.getProperty('namesmatching.url')}api/search?q=${encodedQuery}"
            def resp = webService.getJson(url)
            if (!resp.success) {
                return null
            }

            resp
        }) as Map
    }

    Map searchByVernacularNameOnNameMatchingServer (String name) {
        name = name?.toLowerCase() ?: ""
        cacheService.get('name-matching-server-vernacular-name' + name, {
            def encodedQuery = URLEncoder.encode(name ?: '', "UTF-8")
            def url = "${grailsApplication.config.getProperty('namesmatching.url')}api/searchByVernacularName?vernacularName=${encodedQuery}"
            def resp = webService.getJson(url)
            if (!resp.success) {
                return null
            }

            resp
        }) as Map
    }
}
