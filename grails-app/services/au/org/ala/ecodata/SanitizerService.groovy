package au.org.ala.ecodata


import static org.owasp.html.Sanitizers.*

class SanitizerService {
    static final policy = FORMATTING.and(STYLES).and(LINKS).and(BLOCKS).and(IMAGES).and(TABLES)

    static String sanitize(String input) {
        return policy.sanitize(input)
    }
}