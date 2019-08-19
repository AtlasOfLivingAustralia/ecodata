package au.org.ala.ecodata

import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.DELETED

class MUService {
    
    def commonService

    MU get(String programId, includeDeleted = false) {
        if (includeDeleted) {
            return MU.findByMUId(programId)
        }
        MU.findByMUIdAndStatusNotEqual(programId, DELETED)
    }

    MU findByName(String name) {
        return MU.findByNameAndStatusNotEqual(name, DELETED)
    }

    MU create(Map properties) {

        properties.programId = Identifiers.getNew(true, '')
        MU mu = new MU(MUId:properties.MUId)
        commonService.updateProperties(mu, properties)
        return mu
    }

    MU update(String id, Map properties) {
        MU mu = get(id)
        commonService.updateProperties(mu, properties)
        mu.save(flush:true)
        return mu
    }

    def delete(String id, boolean destroy) {
        MU mu = get(id)
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
                MU.withSession { session -> session.clear() }
                def error = "Error deleting a management unit ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    List<MU> findAllMUsForUser(String userId) {
        List userMUs = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, MU.class.name, DELETED)

        List result = MU.findAllByMUIdInList(userMUs?.collect{it.entityId})
        result
    }

}
