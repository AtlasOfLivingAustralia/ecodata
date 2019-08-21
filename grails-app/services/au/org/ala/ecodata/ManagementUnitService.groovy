package au.org.ala.ecodata

import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.DELETED

class ManagementUnitService {
    
    def commonService

    ManagementUnit get(String muId, includeDeleted = false) {
        if (includeDeleted) {
            return ManagementUnit.findByManagementUnitId(muId)
        }
        return ManagementUnit.findByManagementUnitIdAndStatusNotEqual(muId, DELETED)
    }

    ManagementUnit findByName(String name) {
        return ManagementUnit.findByNameAndStatusNotEqual(name, DELETED)
    }

    ManagementUnit create(Map properties) {

        properties.programId = Identifiers.getNew(true, '')
        ManagementUnit mu = new ManagementUnit(MUId:properties.MUId)
        commonService.updateProperties(mu, properties)
        return mu
    }

    ManagementUnit update(String id, Map properties) {
        ManagementUnit mu = get(id)
        commonService.updateProperties(mu, properties)
        mu.save(flush:true)
        return mu
    }

    def delete(String id, boolean destroy) {
        ManagementUnit mu = get(id)
        if (mu) {
            try {
                if (destroy) {
                    mu.delete()
                } else {
                    mu.status = DELETED
                    mu.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                ManagementUnit.withSession { session -> session.clear() }
                def error = "Error deleting a management unit ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    List<ManagementUnit> findAllManagementUnitsForUser(String userId) {
        List userMUs = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, ManagementUnit.class.name, DELETED)

        List result = ManagementUnit.findAllByMUIdInList(userMUs?.collect{it.entityId})
        result
    }

}
