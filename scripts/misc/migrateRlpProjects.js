load('uuid.js');
// Create new sub-program
var program = db.program.find({name:"Regional Land Partnerships"}).next();

delete program._id;
program.programId = UUID.generate();
program.name = "Regional Land Partnerships - Support";
program.description = "Regional Land Partnerships - Support";

db.program.save(program);

var projects = [
'f3cd85eb-4d68-40ac-8bd9-28d630be2f21',
'7c552737-f1ee-4f77-89ec-eeaac0a48280',
'fe843f31-f094-404c-9c55-b9a177817a66',
'44e26556-3a00-4f8c-abf1-87ad20872050',
'0dbaa0ba-60a9-4277-a909-bd6be69cdae6',
'2a6aa8c9-c552-48f4-a570-28d244b0db8b',
'a3347117-4530-45f4-b173-678557c5b9f5',
'e266ef5b-b437-4d81-b284-6843911db19d',
'c007cf73-d724-4529-bd49-59d4077d01c1',
'fe0a005f-47e7-4fae-b9f2-f03585ddd39a',
'4317e798-805c-4ae1-995f-58ae450f17f6',
'47c64152-1b91-489f-9bb1-e9c3aba799e3',
'208f5743-9012-485c-adc0-4f373aa0c383',
'1f2535c8-63c1-40ee-b404-786dd92a062f',
'5326f27d-56d3-4604-bfc2-b4c91ac83501',
'7e9da744-4783-4a29-b5fb-8d9d4efea707',
'4b2c2d7e-5be6-4c85-a8b6-86ec1e6b95f4'
    ];

var userId = '1493';
var now = ISODate();

for (var i=0; i<projects.length; i++) {
    var project = db.project.find({projectId:projects[i]}).next();
    project.programId = program.programId;
    project.lastUpdated = now;
    db.project.save(project);

    var auditMessage = {
        date:now,
        entity:project,
        eventType:'Update',
        entityType:'au.org.ala.ecodata.Project',
        entityId:projects[i],
        userId:userId
    };

    db.auditMessage.insert(auditMessage);

}