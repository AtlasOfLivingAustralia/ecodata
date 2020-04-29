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

        if (!id) {
            properties.programId = Identifiers.getNew(true, '')
            Program program = new Program(programId: properties.programId)
            commonService.updateProperties(program, properties)
            return program

        } else {
            Program parent = get(id)
            properties.programId = Identifiers.getNew(true, '')
            Program subProgram = new Program(properties)
            subProgram.parent = parent
            subProgram.save(flush: true)
            return subProgram
        }
    }

    Program update(String id, Map properties) {
        String newParentProgramId = properties.parentProgramId
        Program program = get(id)
        Program newParent = get(newParentProgramId)
        program.parent = newParent
        properties.remove('parentProgramId')
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
