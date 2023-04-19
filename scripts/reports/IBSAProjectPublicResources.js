

var programs = ["Land Environmental Assessment Data","Department of Water and Environmental Regulation","Environmental Protection Authority","Department of Mines, Industry Regulation and Safety"];
db.project.find({associatedProgram: {$in: programs}, status: "active"}).forEach(function(project) {

});

var projects = db.project.distinct('projectId', {associatedProgram: {$in: programs}, status:{$ne:'deleted'}});
print("Project id,Project name,Document id,Title,Description,Document type,File size,Content type,URL");
db.document.find({projectId: {$in: projects}, "public": true, status:{$ne:'deleted'}}).forEach(function(document) {
    var project = db.project.find({projectId: document.projectId}).next();
    var path = document.filepath ? document.filepath + "/" : "";
    var url = "https://biocollect.ala.org.au/document/download/" + path + encodeURIComponent(document.filename);
    print(document.projectId + ",\"" + project.name.replace('"', '""') + "\"," + document.documentId + "," + document.name + ",\"" + (document.description || "").replace('"', '""') + "\"," + document.type + "," + document.filesize + "," + document.contentType + "," + url);
});