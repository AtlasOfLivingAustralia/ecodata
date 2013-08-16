package au.org.ala.ecodata

class IdentifierHelper {

    public static String getEntityIdentifier(Object domainObject) {
        def entityId
        switch (domainObject.class.name) {
            case Project.class.name:
                entityId = domainObject.projectId
                break
            case Site.class.name:
                entityId = domainObject.siteId
                break
            case Activity.class.name:
                entityId = domainObject.activityId
                break
            default:
                entityId = domainObject.id
                break
        }
        return entityId
    }

}
