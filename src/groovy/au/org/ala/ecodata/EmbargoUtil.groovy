package au.org.ala.ecodata

import groovy.time.TimeCategory

class EmbargoUtil {

    public static final int MAXIMUM_EMBARGO_PERIOD_MONTHS = 12

    /**
     * Calculate the date when the embargo period ends
     *
     * @param projectActivity Either the ProjectActivity entity, or the Map of its properties
     * @return Null if EmbargoOption.NONE is used, otherwise either the user-entered date when EmbargoOption.DATE is used OR the calculated date when EmbargoOption.DAYS is used
     */
    static Date calculateEmbargoUntilDate(projectActivity) {
        Date embargoUntil = null

        if (projectActivity && projectActivity.visibility) {
            use(TimeCategory) {
                EmbargoOption option = projectActivity.visibility.embargoOption as EmbargoOption
                switch (option) {
                    case EmbargoOption.DAYS:
                        embargoUntil = removeTime(new Date() + projectActivity.visibility.embargoForDays.days)
                        break
                    case EmbargoOption.DATE:
                        embargoUntil = removeTime(projectActivity.visibility.embargoUntil)
                        break
                }

                Date maxAllowed = removeTime(new Date() + MAXIMUM_EMBARGO_PERIOD_MONTHS.months)

                // The requested embargo period should be validated well before this point, but check here to ensure that the basic rules are enforced
                if (embargoUntil > maxAllowed) {
                    throw new IllegalArgumentException("The embargo period cannot be longer than ${MAXIMUM_EMBARGO_PERIOD_MONTHS} months")
                } else if (embargoUntil != null && embargoUntil <= removeTime(new Date())) {
                    throw new IllegalArgumentException("The embargo period must end in the future")
                }
            }
        }

        embargoUntil
    }

    private static Date removeTime(Date date) {
        Date dateOnly = null

        if (date != null) {
            Calendar cal = Calendar.getInstance()
            cal.setTime(date)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            dateOnly = cal.getTime()
        }

        dateOnly
    }
}
