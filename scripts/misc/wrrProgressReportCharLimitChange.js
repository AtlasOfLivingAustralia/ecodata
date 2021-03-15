/***
 * Increase field lengths for WRR Progress Reports
 * Increase to 5000 the char limit for:
     2.1 Project Progress
     2.2 Issues
     3 Good news stories
     4 Monitoring, Evaluation etc
 */

print("******Increase field lengths for WRR Progress Reports******")
var formsList = ['Wildlife Recovery Progress Report - WRR', 'Wildlife Recovery Progress Report - GA' ,
 'Wildlife Recovery Progress Report - CVA']

db.activityForm.find({name: {$in: formsList } }).
 forEach(function(form) {
     print("Form: " + form.name)
     //get the form sections
     var sections = form.sections

    for (var i = 0; i < sections.length; i++) {
        if(sections[i].name === "Wildlife Recovery Progress Report") {
            var fields = sections[i].template.dataModel
            for (var j = 0; j < fields.length; j++) {
                if(fields[j].name === "progressSummary" || fields[j].name === "projectMonitoringAndLearnings"){
                    fields[j].validate = "required,maxSize[5000]"
                    print("Increase field lengths for " + fields[j].name)
                }
                else if(fields[j].name === "issue") {
                    var columns = fields[j].columns
                    for (var x = 0; x < columns.length; x++) {
                        if(columns[x].name === "nature" || columns[x].name === "how" || columns[x].name === "implication"){
                            columns[x].validate = "required,maxSize[5000]"
                            print("Increase field lengths for " + columns[x].name)
                        }
                    }
                }
                else if(fields[j].name === "goodNewsStories") {
                    var columns = fields[j].columns
                    for (var x = 0; x < columns.length; x++) {
                        if(columns[x].name === "storyDetails"){
                            columns[x].validate = "required,maxSize[5000]"
                            print("Increase field lengths for " + columns[x].name)
                        }
                    }
                }
            }
            break;
        }
    }

 db.activityForm.save(form);
})
print("******Completed: Increase field lengths for WRR Progress Reports******")