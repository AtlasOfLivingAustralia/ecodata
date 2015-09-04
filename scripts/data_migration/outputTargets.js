var projects = db.project.find({status:{$ne:'deleted'}});

var duplicateTargets = [
    {
        scoreLabel:'Area covered (Ha) by pest treatment actions',
        duplicate:'Area covered (Ha) by pest treatment'
    },
    {
        scoreLabel:'Area covered (Ha) by pest treatment actions',
        duplicate:'Area covered by pest treatment (Ha)'
    },
    {
        scoreLabel:'Total area prepared (Ha) for follow-up treatment actions',
        duplicate:'Total area prepared for follow-up treatment actions'
    },

    {
        scoreLabel:'Area of land (Ha) changed to sustainable practices',
        duplicate:'Area of land changed to sustainable practices'
    },
    {
        scoreLabel:'Area of land (Ha) on which improved management practices have been implemented',
        duplicate:'Area of land on which improved management practices are implemented'
    },
    {
        scoreLabel:'Area of revegetation works (Ha)',
        duplicate:'Area of works'
    },
    {
        scoreLabel:'Length of stream/coastline treated (Km)',
        duplicate:'Length of stream/coastline treated (m)'
    },
    {
        scoreLabel:'Total length of fence (Km)',
        duplicate:'Total length of fence'
    },
    {
        scoreLabel:'Total No. of individuals or colonies of pest animals destroyed',
        duplicate:'No. of individual animals killed/removed'
    },
    {
        scoreLabel:'Total No. of individuals or colonies of pest animals destroyed',
        duplicate:'No. of individual animals killed/removed by species'
    },
    {
        scoreLabel:'Total No. of community participation and engagement events run',
        duplicate:'Number of Community Participation and Engagement events'
    }

];


function findMatch(targets, scoreLabel) {
    for (var i=0; i<targets.length; i++) {
        if (targets[i].scoreLabel == scoreLabel) {
            return targets[i];
        }
    }
}

while (projects.hasNext()) {
    var project = projects.next();

    if (!project.outputTargets) {
        continue;
    }

    var changed = false;
    for (var i=0; i<duplicateTargets.length; i++) {
        var outputTarget = findMatch(project.outputTargets, duplicateTargets[i].duplicate);

        var correctTarget = findMatch(project.outputTargets, duplicateTargets[i].scoreLabel);

        var output = '';
        if (outputTarget) {
            output = project.projectId+','+outputTarget.outputLabel+','+ outputTarget.scoreLabel+','+ outputTarget.scoreName+','+ outputTarget.target;
            if (correctTarget) {
                output += ',' + correctTarget.outputLabel + ',' + correctTarget.scoreLabel + ',' + correctTarget.scoreName + ',' + correctTarget.target;
            }
            else {
                outputTarget.scoreLabel = duplicateTargets[i].scoreLabel;
                changed = true;
            };
            print(output);
        }

    }
    if (changed) {
        db.project.update({projectId:project.projectId}, {$set:{outputTargets:project.outputTargets}});
    }
}