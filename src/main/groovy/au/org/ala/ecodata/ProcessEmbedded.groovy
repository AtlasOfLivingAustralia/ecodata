package au.org.ala.ecodata

trait ProcessEmbedded {

    void processEmbeddedObjects() {
        List embeddedProps = this.class.declaredFields.findAll {field ->
            if ( this.metaClass.hasProperty(this, 'embedded') ) {
                return field.name in this.class.embedded
            }

            false
        }

        embeddedProps?.each { field ->
            field.setAccessible(true)
            def embeddedObject = field.get(this)
            if (embeddedObject && embeddedObject.metaClass.respondsTo(embeddedObject, 'processEmbeddedObjects')) {
                embeddedObject.processEmbeddedObjects()
            }
            else if (embeddedObject instanceof List) {
                embeddedObject.each { item ->
                    if (item.metaClass.respondsTo(item, 'processEmbeddedObjects')) {
                        item.processEmbeddedObjects()
                    }

                    runLifeCycleListeners(item)
                }
            }

            runLifeCycleListeners(embeddedObject)
        }
    }

    void runLifeCycleListeners(def item) {
        if (item && item.metaClass.respondsTo(item, 'beforeInsert')) {
            item.beforeInsert()
        }

        if (item && item.metaClass.respondsTo(item, 'beforeUpdate')) {
            item.beforeUpdate()
        }
    }

}