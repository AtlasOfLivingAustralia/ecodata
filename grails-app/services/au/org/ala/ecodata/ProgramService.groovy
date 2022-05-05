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
        properties.programId = Identifiers.getNew(true, '')
        Program program = new Program(properties)
        updateParent(program, properties)
        program.save(flush: true)
        return program
    }


    Program update(String id, Map properties) {
        Program program = get(id)
        updateParent(program, properties)
        program.properties = properties
        program.save(flush:true)
        return program
    }

    /**
     * Checks for the presence of a "parentProgramId" key in the properties, and if supplied, updates (or removes)
     * the parent program of the supplied Program.
     * @param program the Program to update
     * @param properties a Map optionally containing a key parentProgramId which specifies the programId of
     * the desired parent program of this program.  A null value is used to indicate this Program should have
     * no parent (i.e. a top level program)
     */
    private void updateParent(Program program, Map properties) {

        final String PARENT_PROGRAM_ID_KEY = "parentProgramId"
        // Some updates are partial updates (e.g. the config), so only attempt to update the parent program
        // if the parentProgramId key is supplied, as otherwise we cannot distinguish a deliberate null
        // from an update that doesn't include the parentProgramId
        if (properties.containsKey(PARENT_PROGRAM_ID_KEY)) {
            String parentProgramId = properties.remove(PARENT_PROGRAM_ID_KEY)
            if (parentProgramId != null) {
                Program newParent = get(parentProgramId)
                program.parent = newParent
            } else {
                program.parent = null
            }
        }
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
        }.toList()

        allProgramList.collect{
            [name:it.name, programId:it.programId, parentId:it.parent?.programId, parentName: it.parent?.name]}
    }


}
