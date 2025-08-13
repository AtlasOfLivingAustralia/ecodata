package au.org.ala.ecodata

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import groovy.util.logging.Slf4j

@Slf4j
class SimpleCacheLogger implements CacheEventListener {

    @Override
    void onEvent(CacheEvent cacheEvent) {
        log.info("Key: {} | EventType: {} | Old value: {} | New hash code: {}",
                cacheEvent.getKey(), cacheEvent.getType(), cacheEvent.getOldValue(),
                cacheEvent.getNewValue().hashCode())

    }
}