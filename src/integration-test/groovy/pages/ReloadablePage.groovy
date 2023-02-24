package pages

import geb.Page

class ReloadablePage extends Page {

    private long atCheckTime = 0l

    static content = {}

    /**
     * Extends the standard at check to set a javascript variable that can later be
     * checked to detect a pageload.
     */
    boolean verifyAt() {
        boolean result = super.verifyAt()
        if (result) {
            saveAtCheckTime()
        }
        result
    }

    def saveAtCheckTime() {
        atCheckTime = System.currentTimeMillis()
        js.exec('window.atCheckTime = '+atCheckTime+';')
    }

    def getAtCheckTime() {
        js.exec('return window.atCheckTime;')
    }


    /** Returns true if the page has been reloaded since the most recent "at" check */
    def hasBeenReloaded() {
        !getAtCheckTime()
    }

    /**
     * Executes this page's "at checker", suppressing any AssertionError that is thrown
     * and returning false.
     *
     * @return whether the at checker succeeded or not.
     * @see #verifyAt()
     */
    boolean verifyAtSafely(boolean honourGlobalAtCheckWaiting = true) {
        boolean result = super.verifyAtSafely(honourGlobalAtCheckWaiting)
        if (result) {
            saveAtCheckTime()
        }
    }

}
