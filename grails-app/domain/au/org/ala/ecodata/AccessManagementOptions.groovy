package au.org.ala.ecodata

/** This is a configuration class that manages the settings for when to expire user access for this hub */
class AccessManagementOptions {

    int expireUsersAfterThisNumberOfMonthsInactive = 24
    int warnUsersAfterThisNumberOfMonthsInactive = 20
}
