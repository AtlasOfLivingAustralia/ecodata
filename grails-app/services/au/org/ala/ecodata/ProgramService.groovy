package au.org.ala.ecodata

import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.DELETED

class ProgramService {
    
    def commonService

    Program get(String programId, includeDeleted = false) {
        if (includeDeleted) {
            return Program.findByProgramId(programId)
        }
        Program.findByProgramIdAndStatusNotEqual(programId, DELETED)
    }
    /**
     *
     * @param ids
     * @return a list of programs
     */
    Program[] get(String[] ids){
        Program[] programs = Program.findAllByProgramIdInList(ids.toList()) //convert to list
        return programs
    }

    Program findByName(String name) {
        return Program.findByNameAndStatusNotEqual(name, DELETED)
    }

    Program create(Map properties) {
        String id = properties.parentProgramId
        properties.programId = Identifiers.getNew(true, '')
        Program program = new Program(properties)
        if (id != null) {
            program.parent = get(id)
        }
        commonService.updateProperties(program, properties)
        program.save(flush: true)
        return program
    }


    Program update(String id, Map properties) {
        String parentProgramId = properties.parentProgramId
        properties.remove('parentProgramId')
        Program program = get(id)
        if (parentProgramId != null){
            Program newParent = get(parentProgramId)
            program.parent = newParent
        }else{
            program.parent = null
        }
        commonService.updateProperties(program, properties)
        program.save(flush:true)
        return program
    }

    List parentNames(Program program) {
        List names = []
        names << program.name
        while (program.parent != null) {
            program = program.parent
            names << program.name
        }
        names
    }

    def delete(String id, boolean destroy) {
        Program program = get(id)
        if (program) {
            try {
                if (destroy) {
                    program.delete()
                } else {
                    program.status = DELETED
                    program.save(flush: true, failOnError: true)
                }
                return [status: 'ok']

            } catch (Exception e) {
                Program.withSession { session -> session.clear() }
                def error = "Error deleting program ${id} - ${e.message}"
                log.error error, e
                def errors = (e instanceof ValidationException)?e.errors:[error]
                return [status:'error',errors:errors]
            }
        } else {
            return [status: 'error', errors: ['No such id']]
        }
    }

    List<Program> findAllProgramsForUser(String userId) {
        List userPrograms = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, Program.class.name, DELETED)

        List result = Program.findAllByProgramIdInList(userPrograms?.collect{it.entityId})
        result
    }

    /**
    * @return All of programs with their name and programId if the program status is not Deleted
     * * */
    List<Map> findAllProgramList() {
        List allProgramList = Program.where {
            status != Status.DELETED
            projections {
                property("name")
                property("programId")
            }
        }.toList()

        allProgramList.collect{[name:it[0], programId:it[1]]}
    }


}
