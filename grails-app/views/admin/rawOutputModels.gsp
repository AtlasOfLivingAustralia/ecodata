<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Output models | Admin | Data capture | Atlas of Living Australia</title>
    </head>

    <body>
        <content tag="pageTitle">Output models</content>
        <content tag="adminButtonBar">
            <button type="button" id="btnSave" %{--data-bind="click:save, disable:modelName() == 'No output selected'"--}% class="btn btn-success">Save</button>
            <button type="button" data-bind="click:revert" class="btn">Cancel</button>
        </content>
        <div class="row-fluid">
            <g:select class="span6" name="outputSelector" from="${activitiesModel.outputs}" optionValue="name"
                      optionKey="template" noSelection="['':'Select an output to edit']"/>
        </div>

        <div class="row-fluid">
            <div class="span12"><h2 data-bind="text:modelName"></h2></div>
        </div>
        <div class="row-fluid">
            <div class="alert" data-bind="visible:transients.hasMessage">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <strong>Warning!</strong> <span data-bind="text:transients.message"></span>
            </div>
        </div>
        <div class="row-fluid">
            <div class="span12">
                %{--<pre id="outputModel" style="margin:0;width:97%;"></pre>--}%
                <textarea id="outputModelEdit" style="width:97%;min-height:600px;"></textarea>
                %{--<label for="dataModel">Data model</label>
                <textarea id="dataModel" style="width:97%;min-height:300px;" class="" data-bind="value:dataModel"></textarea>--}%
                %{--<pre id="dataModel" style="width:97%;min-height:300px;" data-bind="text:ko.toJSON(dataModel,null,2)"></pre>--}%
            </div>
        </div>
        %{--<div class="row-fluid">
            <div class="span12">
                <label for="viewModel">View model</label>
                <textarea id="viewModel" style="width:97%;min-height:300px;" class="" data-bind="value:viewModel"></textarea>
            </div>
        </div>--}%
    <span id="fileupload" class="btn fileinput-button"
          data-url="${createLink(controller: 'admin', action: 'quickStartModel')}">
        <i class="icon-plus"></i> <input type="file" name="file"><span>Quick start model from spreadsheet</span>
    </span>

    <g:if env="development">
        <div class="expandable-debug clearfix">
            <hr />
            <h3>Debug</h3>
            <div>
                <h4>KO model</h4>
                <pre data-bind="text:ko.toJSON($root,null,2)"></pre>
                <h4>Outputs</h4>
                <pre>${activitiesModel.outputs}</pre>
            </div>
        </div>
    </g:if>

<asset:script>
    $(function(){
        var rawData = ${outputData?:'{}'};
        var ViewModel = function () {
            var self = this;
            this.modelName = ko.observable('No output selected');
            this.templateName = ko.observable('');
            this.dataModel = ko.observable();
            this.viewModel = ko.observable();
            this.transients = {};
            this.transients.message = ko.observable('');
            this.transients.hasMessage = ko.computed(function () {return self.transients.message() !== ''});
            this.revert = function () {
                document.location.reload();
            };
            this.save = function () {
                console.log($('#dataModel').val());
                var model = ko.toJS(self);
                delete model.transients;
                console.log("saving " + model.modelName);
                //delete model.transients;
                $.ajax("${createLink(action: 'updateOutputDataModel')}/" + self.templateName(), {
                    type: 'POST',
                    data: $('#dataModel').val(), //ko.toJSON(model, null, 2),
                    contentType: 'application/json',
                    dataType: "json",
                    success: function (data) {
                        if (data.error) {
                            alert(data.message);
                        } else {
                            //alert('saved');
                            //document.location.reload();
                        }
                    },
                    error: function () {
                        alert('failed');
                    }
                });
            };

            this.displayDataModel = function(data) {
                $textarea = $('#outputModelEdit');
                self.modelName(data.modelName);

                self.dataModel(ko.toJSON(data.dataModel, null, 2));
                self.viewModel(ko.toJSON(data.viewModel, null, 2));
                self.transients.message('');

                $textarea.html(vkbeautify.json(data,2));
            };
        },
        // create an empty model so we can bind the message state
        viewModel = new ViewModel();
        ko.applyBindings(viewModel);

        var $pre = $('#outputModel');

        $('#outputSelector').change(function () {
            var output = $(this).val();
            $.getJSON("${createLink(action: 'getOutputDataModel')}/" + output, function (data) {

                $('#hiddenOutputName').val(output);
                if (data.error) {
                    viewModel.modelName(output);
                    viewModel.dataModel("");
                    viewModel.viewModel("");
                    viewModel.transients.message('No existing model was found. You are creating a new model.');
                } else {
                    viewModel.templateName(output);
                    viewModel.displayDataModel(data);
                }
            });
        });

        $('#btnSave').click(function () {
            var output = $('#outputSelector').val();
            $.ajax("${createLink(action: 'updateOutputDataModel')}/" + output, {
                type: 'POST',
                data: $textarea.val(),
                contentType: 'application/json',
                success: function (data) {
                    if (data.error) {
                        alert(data.message);
                    } else {
                        $textarea.html(vkbeautify.json(data,2));
                        document.location.reload();
                    }
                }
            });
        });

        $('#fileupload').fileupload({
            autoUpload:true,
            dataType:'json'
        }).on('fileuploaddone', function(e, data) {

            var result = data.result;

            if (!result) {
                result = {};
                alert('No response from server');
            }
            else {
                if (result.errors) {
                    alert(result.errors);

                }
                viewModel.displayDataModel(result);

            }


        }).on('fileuploadfail', function(e, data) {
            alert(data.errorThrown);
        });



        // open specified output (?open=<templateName> in url)
        var startWith = "${open}";
        if (startWith) {
            $('#outputSelector').val(startWith).change();
        }
    });
</asset:script>
        </body>
</html>