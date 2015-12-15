/*
 * Copyright (C) 2013 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.ecodata

/**
 * Enum for access levels
 *
 * Note: "starred" might need to be moved as its not a perfect fit here
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public enum AccessLevel {
    admin(100),
    caseManager(60),
    editor(40),
    projectParticipant(30),
    starred(20)

    private int code
    private AccessLevel(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
}
