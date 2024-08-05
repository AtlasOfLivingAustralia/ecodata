load('../../utils/audit.js');
let adminUserId = 'system'
let meritHubId = db.hub.findOne({urlPath: "merit"}).hubId;
let projects = db.project.find({hubId:meritHubId, status:{$ne:'deleted'}});
while (projects.hasNext()) {
    let project = projects.next();

    project.associatedOrgs = [];

    // generally orgs are called service providers in the context of procurements and grantees in the
    // context of grants
    let description = null;

    let program = db.program.findOne({programId:project.programId});
    if (!program) {
        print("No program found for project "+project.projectId+" name:"+project.name);
    }
    else {
        if (program.fundingType) {
            switch (program.fundingType) {
                case 'SPP':
                    description = 'Recipient';
                    break;
                case 'Grant':
                    description = 'Grantee';
                case 'Procurement':
                    description = 'Service provider';

            }
        } else if (program.config && program.config.organisationRelationship) {
            description = program.config.organisationRelationship;
        }
    }

    if (!description) {
        if (project.plannedStartDate.getTime() > ISODate('2018-07-01T00:00:00+10:00').getDate()) {
            description = 'Service provider'
        }
        else {
            description = 'Recipient'
        }
    }
    let associatedOrg = {name:project.organisationName, organisationId:project.organisationId, description:description};

    if (!associatedOrg.name) {
        print("No organisation for project "+project.projectId+" name:"+project.name+" organisationId: "+project.organisationId);

    }
    else {
        project.associatedOrgs.push(associatedOrg);

        // For now leave these fields as is to not cause issues when switching branches
        // and to allow this script to be run repeatedly
        //project.organisationId = null;
        //project.organisationName = null;
    }

    if (project.orgIdSvcProvider) {
        let associatedOrg = {name:project.serviceProviderName, organisationId:project.orgIdSvcProvider, description:'Service provider'};
        project.associatedOrgs.push(associatedOrg);
        // For now leave these fields as is to not cause issues when switching branches
        // and to allow this script to be run repeatedly
        //project.orgIdSvcProvider = null;
        //project.serviceProviderName = null;
    }

    db.project.replaceOne({projectId:project.projectId}, project);
    audit(project, project.projectId, 'org.ala.ecodata.Project', adminUserId);

}