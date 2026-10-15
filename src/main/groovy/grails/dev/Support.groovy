package grails.dev

import groovy.transform.CompileStatic

/**
 * Compatibility shim for {@code grails.dev.Support}, which was removed in Grails 7.
 *
 * Legacy Grails 3 plugins (e.g. org.grails.plugins:excel-export:2.1) ship an {@code Application} class whose
 * static initialiser calls {@code Support.enableAgentIfNotPresent(Application)}. These classes are listed as
 * provided artefacts in the plugin descriptor, and grails-quartz's JobArtefactHandler causes them to be
 * initialised during startup, failing with {@code NoClassDefFoundError: grails/dev/Support}.
 *
 * The reloading agent is not used in Grails 8, so this is a no-op.
 * Remove once excel-export has been replaced or upgraded.
 */
@CompileStatic
class Support {

    static void enableAgentIfNotPresent(Class mainClass = null) {
        // no-op: reloading agent support was removed in Grails 7
    }

    static void enableDefaultSecurityManager() {
        // no-op
    }
}

