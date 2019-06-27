<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Edit Form Templates</title>
        <script>
            var fcConfig = {
                activityFormUpdateUrl:"${createLink(controller:'activityForm', action:'update')}",
                getActivityFormUrl: "${createLink(action:'findActivityForm')}",
                newDraftFormUrl:"${createLink(controller:'activityForm', action:'newDraftForm')}",
                publishActivityFormUrl:"${createLink(controller:'activityForm', action:'publish')}",
                unpublishActivityFormUrl:"${createLink(controller:'activityForm', action:'unpublish')}",
                findUsersOfFormUrl:"${createLink(controller:'activityForm', action:'findUsesOfForm')}",
                reloadUrl:"${createLink(controller:'admin', action:'editActivityFormTemplates')}"
            };
        </script>

    </head>

    <body>
        <content tag="pageTitle">Edit form templates</content>
        <content tag="adminButtonBar">
            <button type="button" id="btnSave" data-bind="click:save" class="btn btn-success">Save</button>
            <button type="button" data-bind="click:revert" class="btn">Cancel</button>
        </content>
        <div class="row-fluid form-selection">
            <div class="span6">
                <label>Activity form: <select class="span12" name="formSelector" data-bind="options:activityForms, optionsCaption:'Select a form to edit', optionsText:'name', value:selectedFormName"></select></label>
            </div>
            <div class="span6">
                <label>Version:<br/> <select class="span3" name="versionSelector" data-bind="options:activityFormVersions, value:selectedFormVersion"></select></label>
            </div>
        </div>
        <div class="row-fluid">
            <div class="span12">
                <button type="button" class="btn" data-bind="enable:selectedActivityForm() && selectedActivityForm().publicationStatus == 'published', click:newDraftForm">New draft form</button>
                <button type="button" class="btn" data-bind="enable:selectedActivityForm() && selectedActivityForm().publicationStatus != 'published', click:publishForm">Publish form</button>
                <button type="button" class="btn" data-bind="enable:selectedActivityForm() && selectedActivityForm().publicationStatus == 'published', click:unpublishForm">Un-publish form</button>
                <button type="button" class="btn" data-bind="enable:selectedActivityForm(), click:exportActivity" ><i class="icon-download"></i> Export</button>
                <span class="upload-btn-wrapper">
                    <button class="btn" type="button" data-bind="enable:selectedActivityForm() && selectedActivityForm().publicationStatus != 'published'"><i class="icon-upload"></i> Import</button>
                    <input type="file" id="fileinput" accept="application/json" name="myfile" data-bind="event: {change:importActivity}"/>
                </span>
            </div>
        </div>
        <hr/>
        <div>
            <div class="span6">
                <label>Form section: <select class="span12" name="outputSelector" data-bind="options:selectedActivityForm()?selectedActivityForm().sections:[], optionsText:'name', optionsCaption:'Select an section to edit', value:selectedFormSection"></select></label>
            </div>

        </div>

        <div class="row-fluid">
            <div class="span12"><h2 data-bind="text:modelName"></h2></div>
        </div>
        <div class="row-fluid">
            <div class="alert" data-bind="visible:hasMessage">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <strong>Warning!</strong> <span data-bind="text:message"></span>
            </div>
        </div>
        <div class="row-fluid">
            <div class="alert alert-danger" data-bind="visible:warning()">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <strong>Warning!</strong> <span data-bind="text:warning"></span>
            </div>
        </div>
        <div class="row-fluid">
            <div class="span12">
                <textarea id="outputModelEdit" style="width:97%;min-height:600px;"></textarea>
            </div>
        </div>




<asset:script>
    $(function(){

        var forms = JSON.parse('${(availableActivities as grails.converters.JSON).toString()}');
        var selectedForm = "${params.form}";
        var service = new ActivityFormService(fcConfig);
        var viewModel = new EditActivityFormSectionViewModel(forms, selectedForm, service, fcConfig);
        ko.applyBindings(viewModel);

        $('select').select2();
    });
</asset:script>
        </body>
</html>