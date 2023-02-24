let projects = db.project.find();
const failOnError = true;
while (projects.hasNext()) {
    const project = projects.next();
    let changed = false;
    if (project.outputTargets) {
        if (!project.isMERIT) {
            print("Found biocollect project with output targets! "+project.projectId);
        }
        for (let i=0; i<project.outputTargets.length; i++) {

            let target = project.outputTargets[i];
            if (target.targetDate == "" || target.targetDate === undefined) {
                target.targetDate = null;
                changed = true;
            }
            else if (target.targetDate != null && typeof target.targetDate === 'string') {
                target.targetDate = ISODate(target.targetDate);
                changed = true;
            }


            if (target.target == '') {
                target.target = null;
                changed = true;
            }
            else if (target.target != null && !(typeof target.target == 'object')) {
                print(target.target)
                if (typeof target.target == 'string') {
                    target.target = target.target.replace(",", "");
                    target.target = target.target.replace(' ', "");
                    target.target = target.target.replace('%', "");
                }
                try {
                    target.target = NumberDecimal(target.target);

                }
                catch (e) {
                    print(target.target+" cannot be converted, setting to 0");
                    print(e);
                    if (failOnError) {
                        throw e;
                    }
                    else {
                        target.target = NumberDecimal(0);
                        changed = true;
                    }
                }

            }
            if (project.outputTargets[i].periodTargets) {
                for (var j=0; j<project.outputTargets[i].periodTargets.length; j++) {
                    var pTarget = project.outputTargets[i].periodTargets[j];
                    print(pTarget.target);
                    if (pTarget.target == '') {
                        pTarget.target = null;
                        changed = true;
                    }
                    else if (pTarget.target != null && !(typeof pTarget.target == 'object')) {
                        print(pTarget.target)
                        if (typeof pTarget.target == 'string') {
                            pTarget.target = pTarget.target.replace(",", "");
                            pTarget.target = pTarget.target.replace(' ', "");
                            pTarget.target = pTarget.target.replace('%', "");
                        }
                        pTarget.target = NumberDecimal(pTarget.target);
                        changed = true;
                    }
                }
            }
        }
    }
    if (changed) {
        db.project.replaceOne({_id:project._id}, project);
    }
}
