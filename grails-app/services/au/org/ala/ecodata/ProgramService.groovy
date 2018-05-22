package au.org.ala.ecodata

import grails.validation.ValidationException

import static au.org.ala.ecodata.Status.DELETED

class ProgramService {
    
    def commonService

    def get(String programId, includeDeleted = false) {
        if (includeDeleted) {
            return Program.findByProgramId(programId)
        }
        Program.findByProgramIdAndStatusNotEqual(programId, DELETED)
    }

    Map toMap(Program program, levelOfDetail = []) {
        def dbo = program.dbo
        def mapOfProperties = dbo.toMap()
        mapOfProperties.findAll {k,v -> v != null}
    }

    Program create(Map properties) {

        properties.programId = Identifiers.getNew(true, '')
        Program program = new Program(programId:properties.programId)
        commonService.updateProperties(program, properties)
        return program
    }

    Program update(String id, Map properties) {
        Program program = get(id)
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


}
