package au.org.ala.ecodata

import net.sf.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject


/**
 * Overrides the JSONObject printing / toString mechanism to allow consistent ordering when writing a JSONObject
 * to a String.
 * Primary use is to get consistent ordering in the activities / program models so diffs are readable and not confused
 * by arbitrary re-ordering of attributes between saves.
 */
class OrderedJSONAttributePrinter {

    static Object asType(JSONObject jsonObject, Class c) {
        if (c == String) {
            return toString(jsonObject, 4, 0)
        } else {
            return jsonObject.asType(c)
        }
    }
    /**
     * Make a prettyprinted JSON text of this JSONObject.
     * <p/>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @param indentFactor The number of spaces to add to each level of
     *                     indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *         representation of the object, beginning
     *         with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *         with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws org.codehaus.groovy.grails.web.json.JSONException If the object contains an invalid number.
     */
    static String toString(JSONObject jsonObject, int indentFactor, int indent) throws JSONException {
        int i;
        int n = jsonObject.length();
        if (n == 0) {
            return "{}";
        }
        Set keySet = new TreeSet(jsonObject.keySet())
        Iterator keys = keySet.iterator();
        StringBuilder sb = new StringBuilder("{");
        int newindent = indent + indentFactor;
        Object o;
        if (n == 1) {
            o = keys.next();
            sb.append(jsonObject.quote(o.toString()));
            sb.append(": ");
            sb.append(jsonObject.valueToString(jsonObject.get(o), indentFactor,
                    indent));
        } else {
            while (keys.hasNext()) {
                o = keys.next();
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                for (i = 0; i < newindent; i += 1) {
                    sb.append(' ');
                }
                sb.append(jsonObject.quote(o.toString()));
                sb.append(": ");
                sb.append(jsonObject.valueToString(jsonObject.get(o), indentFactor,
                        newindent));
            }
            if (sb.length() > 1) {
                sb.append('\n');
                for (i = 0; i < indent; i += 1) {
                    sb.append(' ');
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }


}

