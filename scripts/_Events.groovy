/**
 * Created by mol109 on 9/1/17.
 */

eventCreateWarStart = { warName, stagingDir ->
    ant.propertyfile(file: "${stagingDir}/WEB-INF/classes/application.properties") {
        entry(key:"app.build", value: new Date().format("dd/MM/yyyy HH:mm:ss"))
    }
}
