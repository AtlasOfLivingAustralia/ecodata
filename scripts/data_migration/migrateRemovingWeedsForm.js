var outputs = db.output.find({name:'RLP - Weed treatment', status:'active'});


function site(row) {
    return {
        sitesTreated: row.sitesTreated,
        initialOrFollowup: row.initialOrFollowup,
        areaTreatedHa: row.areaTreatedHa,
        lengthTreatedKm: row.lengthTreatedKm,
        weedSpeciesTreated: [weedSpecies(row)]
    };
}

function weedSpecies(row) {
    return {
        weedTargetSpecies:row.weedTargetSpecies,
        threatenedEcologicalCommunity:row.threatenedEcologicalCommunity,
        treatmentMethod:row.treatmentMethod,
        otherTreatmentMethod:row.otherTreatmentMethod,
        treatmentObjective:row.treatmentObjective
    };
}
while (outputs.hasNext()) {

    var output = outputs.next();

    if (!output.outputNotCompleted) {


        var reports = db.report.find({activityId: output.activityId});
        if (reports.count() != 1) {
            throw "Found " + reports.count() + " reports for output: " + output.outputId;
        }
        var report = reports.next();

        if (report.publicationStatus == 'published' || report.publicationStatus == 'pendingApproval') {
            print("WARNING: Report: " + report.name + " for project: " + report.projectId + " has status: " + report.publicationStatus);
        }

        var data = output.data;
        if (data.weedSpeciesTreated && data.weedSpeciesTreated.length > 0) {
            print("Found service for project "+report.projectId+" and report "+report.name+" that needs migration");

            data.weedTreatmentSites = [];
            var currentSite = null;
            for (var i=0; i<data.weedSpeciesTreated.length; i++) {
                var row = data.weedSpeciesTreated[i];
                if (currentSite == null || (row.sitesTreated && row.sitesTreated.featureIds && row.sitesTreated.featureIds.length > 0)) {
                    if (currentSite) {
                        data.weedTreatmentSites.push(currentSite);
                    }

                    currentSite = site(row);

                }
                else {
                    if (currentSite == null) {
                        print( "WARNING: The first row doesn't have a site for project "+report.projectId+" and report "+report.name);
                    }
                    currentSite.weedSpeciesTreated.push(weedSpecies(row));
                }
            }
            data.weedTreatmentSites.push(currentSite);
            delete data.weedSpeciesTreated;

            var userId = '1493';
            var auditMessage = {
                date:ISODate(),
                entity:output,
                eventType:'Update',
                entityType:'au.org.ala.ecodata.Output',
                entityId:output.outputId,
                userId:userId
            };

            db.auditMessage.insert(auditMessage);

            db.output.save(output);

        }


    }
}