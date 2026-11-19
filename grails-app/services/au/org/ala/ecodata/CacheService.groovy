package au.org.ala.ecodata

import org.grails.plugin.cache.GrailsCacheManager
import org.springframework.cache.Cache

import java.time.Duration
import java.time.Instant

/**
 * Handles caching of service responses (after transforming).
 * Remains independent of the source of information by using a closure to generate the cached value.
 */
class CacheService {

    /**
     * The grails cache manager will use the cache configured in 'grails.cache.*'.
     * The default is the Spring Frameworks ConcurrentMapCache.
     * See: https://grails.github.io/grails-cache/snapshot/guide/index.html
     *
     */
    GrailsCacheManager grailsCacheManager

    private static String cacheName = 'ecodata-cache'
    private static long secondsInADay = 24 * 60 * 60

    /**
     * Returns the cached results for the specified key if available and fresh
     * else calls the passed closure to get the results (and cache them).
     * @param key for cache storage
     * @param source closure to retrieve the results if required
     * @param maxAgeInDays the maximum age of the cached results
     * @return the results
     */
    def get(String key, Closure source, float maxAgeInDays = 1.0) {
        def now = Instant.now()

        // return a cached value if it has not expired
        def cachedValue = cache.get(key)
        if (cachedValue != null) {
            def raw = cachedValue.get() as Map
            def expiry = raw?.expiry as Instant
            def resp = raw?.resp
            if (now.isBefore(expiry)) {
                return resp
            } else {
                cache.evict(key)
            }
        }

        // generate and cache the value
        def results
        try {
            results = source.call()
            def hasError = (results?.hasProperty('error') || results?.hasProperty('getError')) && results?.error

            // cache if there is no error
            if (!hasError) {
                def duration = Duration.ofSeconds((maxAgeInDays * secondsInADay) as long)
                def expiry = now + duration
                cache.put(key, [resp: results, expiry: expiry])
            }
        } catch (Exception e) {
            results = [error: e.message]
        }
        return results
    }

    void clear(String key) {
        cache.evict(key)
    }

    void clear() {
        cache.clear()
    }

    private Cache getCache() {
        grailsCacheManager.getCache(cacheName)
    }
}
