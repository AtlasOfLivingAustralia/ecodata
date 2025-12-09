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
enum AccessLevel {
    admin(100),
    caseManager(60),
    // The moderator role is used by BioCollect to represent a user that can manage project data.
    // In the MERIT / Monitor integration it also represents the combined roles of admin and determiner.
    moderator(50),
    editor(40),
    // A determinerParticipant combines the roles of determiner and project participant, which for the MERIT
    // Monitor integration gives them write access to all protocols except for plot selection.
    determinerParticipant(37),
    // A determiner can only perform species identifications/determinations.  It's distinct from moderator as
    // the moderator implies editor (due to the code) and the determinations for MERIT/Monitor are a separate process
    // that doesn't confer project editing rights.
    determiner(35),
    projectParticipant(30),
    readOnly(25),
    starred(20)

    private int code
    private AccessLevel(int c) {
        code = c
    }

    int getCode() {
        return code
    }

    boolean includes(String permission) {
        switch(permission) {
            case 'read':
                return code >= editor.code
            case 'update':
                return code >= editor.code
            case 'administer':
                return code >= admin.code
        }
        false
    }
}
