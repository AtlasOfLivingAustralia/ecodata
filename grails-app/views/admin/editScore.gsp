
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Edit Score</title>
</head>

<body>
    <content tag="pageTitle">Edit Score</content>

    <div>
        <h3 style="display:inline">Edit score</h3>

        <button class="btn btn-success pull-right" data-bind="click:save">Save</button>
    </div>
    <div>
      <label>Label: </label> <input type="text" class="input-xxlarge" data-bind="value:label"/>
    </div>
    <div style="text-align: left;">
        <label>Description:</label>  <textarea rows="3" class="input-xxlarge" data-bind="value:description"></textarea>
    </div>
    <div>
        <label>Category:</label>  <input class="input-xxlarge" type="text" data-bind="value:category"/>
    </div>
    <div>
        <label>Output Type:</label>  <input class="input-xxlarge" type="text" data-bind="value:outputType"/>
    </div>

    <div>
        <label>Units:</label>  <input type="text" data-bind="value:units"/>
    </div>
    <div>
        <label>External ID:</label>  <input type="text" data-bind="value:externalId"/>
    </div>

    <div>
        <label>Display type: </label>
        <select data-bind="value:displayType">
            <option value=""></option>
            <option value="piechart">Pie chart</option>
            <option value="barchart">Bar chart</option>
        </select>
    </div>

    <div>
        <label class="checkbox">Use as output target:
        <input type="checkbox" data-bind="checked:isOutputTarget"/>  </label>
    </div>

    <div>
        <label>Configuration:</label>
        <textarea rows="50" style="width:100%;" data-bind="value:configurationText"></textarea>
    </div>
</li>




<asset:script>


    var ScoreConfigurationModel = function(configuration) {

    };

    var ScoreModel = function (score, config) {
        var self = this;
        self.label = ko.observable(score.label);
        self.description = ko.observable(score.description);
        self.category = ko.observable(score.category);
        self.units = ko.observable(score.units);
        self.outputType = ko.observable(score.outputType);
        self.externalId = ko.observable(score.externalId);
        self.entityTypes = ko.observableArray(score.entityTypes);
        self.displayType = ko.observable(score.displayType);
        self.isOutputTarget = ko.observable(score.isOutputTarget);
        self.configurationText = ko.observable(vkbeautify.json(JSON.stringify(score.configuration),2));
        self.entity = ko.observable(score.entity || 'Activity');


        self.save = function () {
            var model = ko.toJS(self);
            model.configuration = JSON.parse(model.configurationText);
            delete model.configurationText;
            var data = JSON.stringify(model);

            delete model.transients;
            $.ajax(config.updateScoreUrl, {
                type: 'POST',
                data: data,
                dataType:'json',
                contentType: 'application/json',
                success: function (data) {
                    if (data !== 'error') {
                        alert('saved');
                        document.location.href = editScoreUrl+'/'+data.scoreId+'.json';
                    } else {
                        alert(data);
                    }
                },
                error: function () {
                    alert('failed');
                }
            });
        };
    };

    var score = JSON.parse('${score as grails.converters.JSON}');
    var updateScoreUrl = '${g.createLink(controller: 'admin', action:'updateScore', id:score.scoreId?:'')}';
    var editScoreUrl = '${g.createLink(action:'editScore')}';
    var model = new ScoreModel(score, {updateScoreUrl:updateScoreUrl+'.json', editScoreUrl:editScoreUrl});
    ko.applyBindings(model);

</asset:script>
   </body>
</html>