package au.org.ala.ecodata

import com.mongodb.client.MongoCollection
import grails.gorm.DetachedCriteria
import org.bson.Document
import org.bson.conversions.Bson
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.SessionImplementor
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.engine.AbstractMongoObectEntityPersister
import org.springframework.validation.Errors

class GormMongoUtil {

    /**
     * Returns the native MongoDB Document backing a GORM entity (including dynamic attributes and any
     * fields stored in the database but not mapped on the domain class).
     *
     * Grails 8 removed the deprecated MongoEntity.getDbo() method, so entity.getProperty("dbo") now resolves to
     * an (absent) dynamic attribute and returns null. This reproduces the Grails 7 getDbo() behaviour using
     * the public GORM session APIs.
     *
     * As with the Grails 7 entity.getProperty("dbo") call, a getDbo() method registered on the entity's metaClass
     * takes precedence (unit tests use this to supply the document for transient instances).
     */
    static Map getDbo(Object entity) {
        if (entity == null) {
            return null
        }
        MetaMethod getDboOverride = entity.metaClass.getMetaMethod('getDbo', [] as Object[])
        if (getDboOverride != null) {
            return (Map) getDboOverride.invoke(entity, [] as Object[])
        }
        AbstractMongoSession session = (AbstractMongoSession) AbstractDatastore.retrieveSession(MongoDatastore)
        SessionImplementor<Document> si = (SessionImplementor<Document>) session
        PersistentEntity persistentEntity = session.mappingContext.getPersistentEntity(entity.getClass().name)

        // check first for embedded cached entries
        Document dbo = (Document) si.getCachedEntry(persistentEntity, NativeEntryEntityPersister.createEmbeddedCacheEntryKey(entity))
        if (dbo != null) {
            return dbo
        }
        // otherwise check if instance is contained within session
        if (!session.contains(entity)) {
            dbo = new Document()
            si.cacheEntry(persistentEntity, AbstractMongoObectEntityPersister.createInstanceCacheEntryKey(entity), dbo)
            return dbo
        }

        EntityPersister persister = (EntityPersister) session.getPersister(entity)
        Serializable id = (Serializable) persister.getObjectIdentifier(entity)
        dbo = (Document) si.getCachedEntry(persister.getPersistentEntity(), id)
        if (dbo == null) {
            MongoCollection<Document> coll = session.getCollection(persistentEntity)
            dbo = coll.find((Bson) new Document(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)).limit(1).first()
        }
        dbo
    }

    // convert object to map
    @Deprecated
    /** The dbo property of domain objects is deprecated */
    static Map extractDboProperties(obj) {
        obj.collectEntries { field ->
            [field.key, field.value]
        }
    }

    // Deep Prune a hash of all empty [:], [] preserving false and other invalid values
    static Map deepPrune(Map map) {
        map.collectEntries { k, v ->
            [k, v instanceof Map ? deepPrune(v) : v]
        }.findAll { k, v -> v != [:] && v != [] && v != null && v != 'null' && !(v instanceof org.bson.BsonUndefined) }
    }

    /**
     * Helper method to compare the value of a property with the value currently stored in the database and
     * reject it if it is different.
     * If the entity is not currently in the database, or the value of the property in the database is null
     * this method will not result in a new Error.
     * Otherwise, an error will be added with the key "<entity class name>.<property name>.cannotBeReassigned"
     * @param entity The entity instance to validate
     * @param identifier The unique id to use when querying the database
     * @param writeOnceProperty The property to validate
     * @param errors THe Errors object to use.
     */
    static void validateWriteOnceProperty(Object entity, String identifier, String writeOnceProperty, Errors errors) {
        if (entity[identifier]) {

            // A criteria query is used to bypass any caching that occurs by GORM. (e.g. entity.findByXXX will use
            // the cache if available)
            Object storedPropertyValue = new DetachedCriteria(entity.class).get {
                eq(identifier, entity[identifier])
                projections {
                    property(writeOnceProperty)
                }
            }

            if (storedPropertyValue && storedPropertyValue != entity[writeOnceProperty]) {
                errors.rejectValue(writeOnceProperty, "${entity.class.name}.${writeOnceProperty}.cannotBeReassigned")
            }
        }
    }

}
