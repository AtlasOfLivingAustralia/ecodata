package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.DateUtil

/***
 * This class is used to convert the ISO date string to yyyy-MM-dd format
 */
class DateTimeFormatting extends DateFormatting {

    protected Date parse(String dateStr ) {
        DateUtil.parseDateWithAndWithoutMilliSeconds(dateStr)
    }

    protected String format(Date date) {
        DateUtil.format(date)
    }

}
