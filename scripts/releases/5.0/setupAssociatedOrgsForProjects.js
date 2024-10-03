load('../../utils/audit.js');
let adminUserId = 'system'
let meritHubId = db.hub.findOne({urlPath: "merit"}).hubId;
let projects = db.project.find({hubId:meritHubId, status:{$ne:'deleted'}});
while (projects.hasNext()) {
    let project = projects.next();

    project.associatedOrgs = [];

    // generally orgs are called service providers in the context of procurements and grantees in the
    // context of grants
    let description = 'Recipient';
    let associatedOrg = {name:project.organisationName, organisationId:project.organisationId, description:description};

    if (project.organisationId) {
        let organisation = db.organisation.findOne({organisationId:project.organisationId});
        if (!organisation) {
            print("OrganisationId "+project.organisationId+" not found for project "+project.projectId+" name:"+project.name);
        }
        else {
            associatedOrg.organisationName = organisation.name;
        }
    }


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

        let organisation = db.organisation.findOne({organisationId:project.orgIdSvcProvider});
        if (!organisation) {
            print("OrganisationId "+project.orgIdSvcProvider+" not found for project "+project.projectId+" name:"+project.name);
        }
        else {
            associatedOrg.organisationName = organisation.name;
        }


        project.associatedOrgs.push(associatedOrg);
        // For now leave these fields as is to not cause issues when switching branches
        // and to allow this script to be run repeatedly
        //project.orgIdSvcProvider = null;
        //project.serviceProviderName = null;
    }

    db.project.replaceOne({projectId:project.projectId}, project);
    audit(project, project.projectId, 'org.ala.ecodata.Project', adminUserId);

}