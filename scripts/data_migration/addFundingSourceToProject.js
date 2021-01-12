/**
 * This is javascript look in to
 * @project
 * @funding amount and also check in the project
 * @IsMERIT true
 * @programs: try to look for a programID in program collection in db
 * @acronym RLP then it will set the
 * @fundingSource RLP otherwise NON-RLP
 */
const allProjects = db.project.find({funding:{$gt:0}, isMERIT:true});

allProjects.forEach(function (projects){
    if (projects){
        if (!projects.fundings){
            if (projects.programId){
                let program = db.program.find({programId: projects.programId});
                if (program){
                    program.forEach( function (programs) {
                        if (programs.programId === projects.programId){
                            if (programs.acronym ==="RLP"){
                                print("RLP Programs Id: " + programs.programId + " " + "Project ID: " + projects.projectId);
                                     db.project.update({projectId:projects.projectId},{$set:{fundings:[{fundingType:"Public - commonwealth", fundingSource:"RLP", fundingSourceAmount:projects.funding}]}});
                            }else{
                                print("Non RLP Program ID: " + programs.programId + " " + "Project ID: " + projects.projectId);
                              db.project.update({projectId:projects.projectId},{$set:{fundings:[{fundingType:"Public - commonwealth", fundingSource:"NON-RLP", fundingSourceAmount:projects.funding}]}});
                            }
                        }
                    });
                }
            }else{

                print("Non RLP Projects without Program Id: Project Id: " + projects.projectId);
              db.project.update({projectId:projects.projectId},{$set:{fundings:[{fundingType:"Public - commonwealth", fundingSource:"NON-RLP", fundingSourceAmount:projects.funding}]}});
            }
        }else{
            // if projects.fundings exist.
            print("Fundings id Already exists for this projects:  " + projects.projectId);
        }
    }else{
        print("Not Project Found");
    }

});
