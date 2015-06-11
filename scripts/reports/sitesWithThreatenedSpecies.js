//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Species'});
var projects = db.project.find({status:{$ne:'deleted'}, $or:[{'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'}, {'custom.details.objectives.rows1.assets':'Threatened Species'}]});
//var projects = db.project.find({status:{$ne:'deleted'}, 'custom.details.objectives.rows1.assets':'Threatened Ecological Communities'});

//print('Name, Grant ID, External ID, Organisation Name, Programme, Sub-Programme, Planned Start Date, Planned End Date, Funding, Status, Description, State, State, State');

print('Project ID,Grant ID,External ID,Sub Programme,Site,lat,lon,WKT,pid,fid,source,type');

while (projects.hasNext()) {
    var project = projects.next();

    var sites = db.site.find({projects:project.projectId, status:{$ne:'deleted'}});
    while (sites.hasNext()) {
        var site = sites.next();
        if (site.extent && site.extent.geometry && site.extent.geometry.fid != 'cl22') {
            var programSubProgram = project.associatedProgram + ' - ' + project.associatedSubProgram;
            var lat = site.extent.geometry.centre[1];
            var lon = site.extent.geometry.centre[0];

            var source = site.extent.source;
            var type = site.extent.geometry.type;
            var WKT = '';
            switch (source) {
                case 'point':
                    WKT = 'POINT('+lon+' '+lat+')';
                    break;
                case 'drawn':
                    switch (type) {
                        case 'Point':
                            WKT = 'POINT('+lon+' '+lat+')';
                            break;
                        case 'Polygon':
                            var WKT;
                            if (site.extent.geometry.coordinates[0].length < 4) {
                                WKT = 'MULTILINESTRING(('
                            }
                            else {

                                WKT = 'MULTIPOLYGON(((';
                            }

                            for (var i = 0; i < site.extent.geometry.coordinates[0].length; i++) {
                                if (i != 0) {
                                    WKT+=','
                                }
                                var point = site.extent.geometry.coordinates[0][i];
                                WKT = WKT + point[0] + ' ' + point[1];
                            }
                            if (site.extent.geometry.coordinates[0].length < 4) {
                                WKT += '))';
                            }
                            else {
                                WKT += ')))';
                            }

                            break;
                    }
                    break;
            }
            var pid = site.extent.geometry.pid;


            print(project.projectId+','+project.grantId+','+project.externalId+',"'+programSubProgram+'",https://fieldcapture.ala.org.au/site/index/'+site.siteId+','+lat+','+lon+',"'+WKT+'",'+pid+','+site.fid+','+source+','+site.extent.geometry.type);

        }
    }
}
