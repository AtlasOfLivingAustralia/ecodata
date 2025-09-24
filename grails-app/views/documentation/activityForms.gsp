<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="api"/>
    <title>Ecodata activity forms</title>
</head>

<body>

<div class="container-fluid api">

    <div class="row">
        <h3>Activities and Output Descriptions</h3>
        <p>
            In the MERIT and BioCollect systems, each project has a collection of Activities.  Each activity in turn has a collection of Outputs.
        </p>
        <h4>Activities</h4>
        <table class="table table-striped">
            <thead>
            <tr>
                <th class="api-method">Name</th><th class="api-description"><g:message code="api.property.description.header" default="Definition"/></th>
            </tr>
            </thead>
            <tbody>
                <g:each in="${activitiesModel.activities}" var="activity">
                    <tr>
                        <td>
                            <g:link action="activity" id="${activity.name+'.html'}">${activity.name}</g:link>
                        </td>
                        <td>
                            <g:message code="${'api.'+activity.name+'.description'}" default="${g.message([code:'api.description.missing'])}"/>
                        </td>
                    </tr>
                </g:each>

            </tbody>
        </table>
        <h4>Outputs</h4>
        <p>The scores below are values configured from the details collected for an output that are currently used for reporting.  Scores in <b>bold</b> can be assigned as project as output targets.</p>
        <table class="table table-striped">
            <thead>
            <tr>
                <th class="output-name">Name</th><th class="output-description"><g:message code="api.property.description.header" default="Definition"/></th><th class="output-scores">Scores</th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${activitiesModel.outputs}" var="output">
                <tr>
                    <td>
                        <g:link action="output" id="${(output.name+'.html').encodeAsURL()}">${output.name}</g:link>
                    </td>
                    <td>
                        <g:message code="${'api.'+output.name+'.description'}" default="${g.message([code:'api.description.missing'])}"/>
                    </td>
                    <td>
                        <ul>
                        <g:each var="score" in="${scores[output.name]}">
                            <li><g:if test="${score.isOutputTarget}"><b></g:if>${score.label} <g:if test="${score.units}">(${score.units})</g:if><g:if test="${score.isOutputTarget}"></b></g:if></li>
                        </g:each>
                        </ul>
                    </td>
                </tr>
            </g:each>

            </tbody>
        </table>
    </div>
</div>

</body>
</html>
