package au.org.ala.ecodata.validation

import au.org.ala.ecodata.InvestmentPriority
import au.org.ala.ecodata.Status
import grails.compiler.GrailsCompileStatic
import org.springframework.validation.AbstractBindingResult
import org.springframework.validation.FieldError

/**
 * Verifies that any Investment Priorities recorded in a Project exist in the database.
 */
@GrailsCompileStatic
class InvestmentPriorityValidator {

    static void validateInvestmentPriorities(String path, List<String> investmentPriorityIds, AbstractBindingResult errors) {

        if (!investmentPriorityIds) {
            return
        }
        List<InvestmentPriority> investmentPrioritiesInDatabase = InvestmentPriority.findAllByInvestmentPriorityIdInList(investmentPriorityIds)

        List<String> missingIds = investmentPriorityIds - investmentPrioritiesInDatabase.collect{it.investmentPriorityId}
        List<InvestmentPriority> deletedInvestmentPriorities = investmentPrioritiesInDatabase.findAll{it.status == Status.DELETED}

        if (missingIds) {
            errors.addError(new FieldError(
                    'project',
                    path,
                    missingIds,
                    false,
                    ['investmentPriority.notFound'] as String[],
                    missingIds as Object[],
                    "Invalid investment priorities: ${missingIds.join(',')}".toString()))
        }
        if (deletedInvestmentPriorities) {
            String ids = deletedInvestmentPriorities.collect{it.investmentPriorityId}.join(', ')
            errors.addError(new FieldError(
                    'project',
                    path,
                    ids,
                    false,
                    ['investmentPriority.deleted'] as String[],
                    [ids] as Object[],
                    "Investment priorities have been deleted: ${deletedInvestmentPriorities.collect{it.name}}".toString()))
        }
    }

}
