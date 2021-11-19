package au.org.ala.ecodata

class IdentifierHelper {

    static String getEntityIdentifier(Map obj) {
        getEntityIdentifier(obj, obj['className'])
    }

    static String getEntityIdentifier(Object obj) {
        getEntityIdentifier(obj, obj.getClass().name)
    }
    static String getEntityIdPropertyName(String className) {
        String propertyName
        switch (className) {
            case Project.class.name:
                propertyName = 'projectId'
                break
            case Site.class.name:
                propertyName = 'siteId'
                break
            case Activity.class.name:
                propertyName = 'activityId'
                break
            case Output.class.name:
                propertyName = 'outputId'
                break
            case Document.class.name:
                propertyName = 'documentId'
                break
            case Score.class.name:
                propertyName = 'scoreId'
                break
            case UserPermission.class.name:
                propertyName = 'id'
                break
            case Program.class.name:
                propertyName = 'programId'
                break
            case Organisation.class.name:
                propertyName = 'organisationId'
                break
            case Report.class.name:
                propertyName = 'reportId'
                break
            case Record.class.name:
                propertyName = 'occurrenceID'
                break
            case Lock.class.name:
                propertyName = 'id'
                break
        }
        propertyName
    }

    static String getEntityIdentifier(Object obj, String className) {
        String entityId
        switch (className) {
            case Project.class.name:
                entityId = obj.projectId
                break
            case Site.class.name:
                entityId = obj.siteId
                break
            case Activity.class.name:
                entityId = obj.activityId
                break
            case Output.class.name:
                entityId = obj.outputId
                break
            case Document.class.name:
                entityId = obj.documentId
                break
            case Score.class.name:
                entityId = obj.scoreId
                break
            case UserPermission.class.name:
                entityId = obj.id?.toHexString() ?: ''
                break
            case Program.class.name:
                entityId = obj.programId
                break
            case Organisation.class.name:
                entityId = obj.organisationId
                break
            case Report.class.name:
                entityId = obj.reportId
                break
            case Record.class.name:
                entityId = obj.occurrenceID
                break
            case Lock.class.name:
                entityId = obj.id
                break
            default:
                // Last chance to find a 'real' entity id, rather than the internal mongo id.
                // try a synthesized id member user the <class name>Id pattern
                entityId = getIdPropertyValue(obj)
                if (!entityId) {
                    entityId = obj.id

                }
                break
        }
        return entityId
    }

    /**
     * If the supplied obj is associate with a project, this method will return the projectId of that Project.
     * Otherwise null will be returned.
     * @param domainObject the domain object
     * @return the associated projectId or null if the obj is not associated with a Project.
     */
    public static String getProjectId(Object domainObject) {
        String projectId = null
        switch (domainObject.class.name) {
            case UserPermission.class.name:
                if (domainObject.entityType == Project.class.name) {
                    projectId = domainObject.entityId
                }
                break
            default:
                if (domainObject.hasProperty('projectId')) {
                    projectId = domainObject.projectId
                }
                break
        }
        return projectId
    }

    private static String getIdPropertyValue(Object object) {
        if (object) {
            def name = object.class.simpleName
            def idMemberName = name[0].toLowerCase() + name.substring(1) + "Id"
            if (object.hasProperty(idMemberName)) {
                return object[idMemberName]
            }
        }
        return null
    }

}
