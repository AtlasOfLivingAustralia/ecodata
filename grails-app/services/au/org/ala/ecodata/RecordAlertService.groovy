/*
 * Copyright (C) 2016 Atlas of Living Australia
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

/*
 * Handles record alert notification
 */
class RecordAlertService {

    ProjectActivityService projectActivityService
    EmailService emailService
    ProjectService projectService

    def groovyPageRenderer
    def grailsApplication

    static transactional = false

    /**
     * Send email notification to species subscribers
     * Checks whether email alert is required based on project activity alert settings
     *
     * @param record record object
     * @return void
     */
    void alertSubscribers(Record record) {

        def pActivity = projectActivityService.get(record?.projectActivityId)
        def project = projectService.get(record?.projectId)

        if(!project?.isMerit && isAlertRequired(pActivity, record?.guid)) {
            Map values = [:]
            values.scientificName = record.scientificName
            values.name = record.name
            values.guid = record.guid
            values.occurrenceID = record.occurrenceID
            values.pActivityName = pActivity?.name
            values.projectName = project?.name
            values.activityUrl = grailsApplication.config.biocollect.activity.url + record?.activityId
            values.projectUrl = grailsApplication.config.biocollect.project.url + project?.projectId

            String body = groovyPageRenderer.render(template: "/email/speciesAlert", model:[values: values])
            emailService.sendEmail("Species Alert", body, pActivity?.alert?.emailAddresses?.collect{it})
        }
    }

    /**
     * Checks whether email alert is required based on project activity alert settings
     *
     * @param pActivity project activity map
     * @param guid species guid.
     * @return void
     */
    private boolean isAlertRequired(Map pActivity, String guid) {
        pActivity?.alert?.allSpecies?.size() > 0 && pActivity?.alert?.emailAddresses?.size() > 0 && pActivity?.alert?.allSpecies?.find {
            it.guid == guid
        }
    }

}
