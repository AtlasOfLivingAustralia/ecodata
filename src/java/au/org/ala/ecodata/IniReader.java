/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.ecodata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 * Copied from layers-store project. Original location is
 * org.ala.layers.grid in the layers-store project
 *
 * Provides read only access to an ini file.
 *
 * File format expected is:
 * <code>
 * [section_name]
 * key_name=key_value
 * </code>
 * where key_values are able to be returned when a
 * section_name and key_name are provided.
 *
 * Errors and absences result in default values returned
 * from get functions.
 *
 * @author Adam Collins
 */
public class IniReader {

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    /**
     * store for ini data after loading
     * <li>map key is concat of section_name + "\\" + key_name
     * <li>map object is key value as a String
     */
    java.util.HashMap<String, String> document;

    /**
     * Constructor loads ini file into the document object.
     * Any errors or failure will log an error only.
     * @param filename ini file to load
     */
    public IniReader(String filename) {
        document = new java.util.HashMap<String, String>();
        loadFile(filename);
    }

    /**
     * errors result in a log of the error only
     * @param filename file to load into document object
     */
    private void loadFile(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String currentSection = "";
            String key;
            String value;
            int i;
            while (in.ready()) {
                String line = in.readLine().trim();//don't care about whitespace
                // ignore the comments
                if (line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.length() > 2 && line.charAt(0) == '[') {
                    i = line.lastIndexOf("]");
                    if (i <= 0 && line.length() > 1) { //last brace might be missing
                        currentSection = line.substring(1);
                    } else if (i > 2) { //empty section names are ignored
                        currentSection = line.substring(1, i);
                    }
                } else if (line.length() > 2) {
                    key = "";
                    value = "";
                    i = line.indexOf("="); //rather than split incase value contains '='
                    if (i > 1) {
                        key = line.substring(0, i);
                    }
                    if (i < line.length() - 1) {
                        value = line.substring(i + 1);
                    }
                    //do not add if key is empty
                    document.put(currentSection + "\\" + key, value);

                }
            }
            in.close();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }
    }

    /**
     *
     * @param section section name as String
     * @param key key name as String
     * @return value of key as String
     * 	empty string when key is not found
     */
    public String getStringValue(String section, String key) {
        String ret = document.get(section + "\\" + key);
        if (ret == null) {
            ret = "";
        }
        return ret;
    }

    /**
     *
     * @param section section name as String
     * @param key key name as String
     * @return value of key as int
     * 	0 when key is not found
     */
    public int getIntegerValue(String section, String key) {
        String str = document.get(section + "\\" + key);
        Integer ret;
        try {
            ret = new Integer(str);
        } catch (Exception e) {
            ret = new Integer(0);
        }
        return ret.intValue();
    }

    /**
     *
     * @param section section name as String
     * @param key key name as String
     * @return value of key as double
     * 	0 when key is not found
     */
    public double getDoubleValue(String section, String key) {
        String str = document.get(section + "\\" + key);
        Double ret;
        try {
            ret = new Double(str);
        } catch (Exception e) {
            ret = new Double(0);
        }
        return ret.doubleValue();
    }

    /**
     * @param section
     * @param key
     * @return true if value was loaded from the ini file
     * 	false if the value was not loaded from the ini file
     */
    public boolean valueExists(String section, String key) {
        return document.get(section + "\\" + key) != null;
    }

    public void setValue(String section, String key, String value) {
        document.put(section + "\\" + key, value);
    }

    public void write(String filename) {
        write(document, filename);
    }

    public void write(Map<String, String> doc, String filename) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

            TreeMap<String, String> pmap = new TreeMap<String, String>(doc);
            Iterator<String> it = pmap.keySet().iterator();
            String currentSection = "";
            while (it.hasNext()) {
                String key = it.next();
                String[] sectionkey = key.split("\\\\");
                if (!currentSection.equals(sectionkey[0])) {
                    currentSection = sectionkey[0];
                    out.println("\n");
                    out.println("[" + sectionkey[0] + "]");
                }
                out.println(sectionkey[1] + "=" + pmap.get(key));
            }
            out.close();

        } catch (Exception e) {
            logger.error("Unable to write ini to " + filename);
            e.printStackTrace(System.out);
        }
    }
}

