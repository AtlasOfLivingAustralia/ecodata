
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Edit Score</title>
</head>

<body>
    <content tag="pageTitle">Edit Score</content>
    <form>
    <div class="my-2">
        <h3 style="display:inline">Edit score</h3>

        <button class="btn btn-success pull-right" data-bind="click:save">Save</button>
    </div>
    <div class="my-2">
      <label class="form-label">Label: </label> <input type="text" class="form-control" data-bind="value:label"/>
    </div>
    <div  class="my-2">
        <label class="form-label">Description:</label>  <textarea rows="3" class="form-control" data-bind="value:description"></textarea>
    </div>
    <div class="my-2">
        <label class="form-label">Category:</label>  <input class="form-control" type="text" data-bind="value:category"/>
    </div>
    <div class="my-2">
        <label class="form-label">Output Type:</label>  <input class="form-control" type="text" data-bind="value:outputType"/>
    </div>

    <div class="my-2">
        <label class="form-label">Units:</label>  <input class="form-control" type="text" data-bind="value:units"/>
    </div>
    <div>
        <label class="form-label">External ID:</label>  <input class="form-control" type="text" data-bind="value:externalId"/>
    </div>

    <div class="my-2">
        <label class="form-label">Display type: </label>
        <select class="form-control" data-bind="value:displayType">
            <option value=""></option>
            <option value="piechart">Pie chart</option>
            <option value="barchart">Bar chart</option>
        </select>
    </div>

    <div class="my-2 form-check">
        <label class="form-check-label">Use as output target:</label>
        <input type="checkbox" class="form-check-input" data-bind="checked:isOutputTarget"/>
    </div>

    <div class="my-2">
        <label>Configuration:</label>
        <div id="score-configuration"></div>
    </div>
    </form>


<asset:javascript src="bundle-vendor"/>
<asset:javascript src="bundle-runtime"/>
<asset:javascript src="bundle-score"/>

<asset:script>

    var score = JSON.parse('${raw((score as grails.converters.JSON).toString())}');
    var updateScoreUrl = '${g.createLink(controller: 'admin', action:'updateScore', id:score.scoreId?:'')}';
    var editScoreUrl = '${g.createLink(action:'editScore')}';
    var model = new LibraryScore.ScoreModel(score, {updateScoreUrl:updateScoreUrl, editScoreUrl:editScoreUrl, scoreEditorId:'score-configuration'});
    ko.applyBindings(model);

</asset:script>
   </body>
</html>