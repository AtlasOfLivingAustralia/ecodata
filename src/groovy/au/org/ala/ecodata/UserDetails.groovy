package au.org.ala.ecodata

class UserDetails {

    public static final String REQUEST_USER_DETAILS_KEY = 'ecodata.request.user.details'

    String displayName
    String userName
    String userId

    @Override
    public String toString() {
        "[ userId: ${userId}, userName: ${userName}, displayName: ${displayName} ]"
    }
}
