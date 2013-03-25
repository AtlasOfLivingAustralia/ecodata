package au.org.ala.ecodata

/**
 * Created with IntelliJ IDEA.
 * User: Mark
 * Date: 14/03/13
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
class Coordinate {
    String decimalLatitude
    String decimalLongitude
    String uncertainty
    String precision
    String datum

    static mapping = {
        version false
    }

    static constraints = {
        decimalLatitude nullable:true
        decimalLongitude nullable:true
        uncertainty nullable:true
        precision nullable:true
        datum nullable:true
    }
}