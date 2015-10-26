package au.org.ala.ecodata

import groovy.time.TimeCategory

class EmbargoUtil {

    public static final int MAXIMUM_EMBARGO_PERIOD_MONTHS = 12

    static Date calculateEmbargoUntilDate(ProjectActivity projectActivity) {
        Date embargoUntil = null

        if (projectActivity) {
            use(TimeCategory) {
                switch (projectActivity.embargoOption) {
                    case EmbargoOption.DAYS:
                        embargoUntil = removeTime(new Date() + projectActivity.embargoForDays.days)
                        break
                    case EmbargoOption.DATE:
                        embargoUntil = removeTime(projectActivity.embargoUntil)
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
