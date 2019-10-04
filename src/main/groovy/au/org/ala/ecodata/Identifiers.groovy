package au.org.ala.ecodata

/**
 * Created with IntelliJ IDEA.
 * User: Mark
 * Date: 28/03/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
class Identifiers {

    static getNew(useUUID, str) {
        if (useUUID || !str) {
            return UUID.randomUUID().toString()
        } else {
            return UUID.nameUUIDFromBytes(str as byte[]).toString()
        }
    }
}
