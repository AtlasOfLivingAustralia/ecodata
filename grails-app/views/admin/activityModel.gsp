<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Activity model - Admin - Data capture - Atlas of Living Australia</title>
    <script>
        var fcConfig = {
            activityFormUpdateUrl: "${createLink(controller:'activityForm', action:'update')}",
            activityFormCreateUrl: "${createLink(controller:'activityForm', action:'create')}",
            getActivityFormUrl: "${createLink(action:'findActivityForm')}"
        };
    </script>

</head>

<body>
<content tag="pageTitle">Activity model</content>
<content tag="adminButtonBar">
    <button type="button" data-bind="click:save, enable:selectedActivity()" class="btn btn-success">Save</button>
    <button type="button" data-bind="click:revert" class="btn">Cancel</button>
</content>

<div class="row-fluid form-selection" data-bind="with:selectionModel">
    <div class="span6">
        <label>Activity form: <select class="span12" name="formSelector"
                                      data-bind="options:activityForms, optionsCaption:'Select a form to edit', optionsText:'name', value:selectedFormName"></select>
        </label>
    </div>

    <div class="span6">
        <label>Version:<br/> <select class="span3" name="versionSelector"
                                     data-bind="options:activityFormVersions, value:selectedFormVersion"></select>
        </label>
    </div>
</div>

<div class="row-fluid clearfix">
    <div class="span12">
        <ul data-bind="with:selectedActivity" class="activityList sortableList">

            <li class="item" data-bind="css:{disabled:!enabled()}">

                <div data-bind="click:toggle"><h3 data-bind="text:name"></h3></div>

                <div class="details clearfix">
                    <div data-bind="template: {name: displayMode}"></div>
                </div>
            </li>
        </ul>
        <span data-bind="click:addActivity, visible:!selectedActivity()" class="clickable"><i
                class="icon-plus"></i> Add new</span>
    </div>

</div>


<script id="viewActivityTmpl" type="text/html">
<div>Type: <span data-bind="text:type"></span></div>

<div>Category: <span data-bind="text:category"></span></div>

<div>Enabled: <span data-bind="text:enabled"></span></div>

<div>GMS ID: <span data-bind="text:gmsId"></span></div>

<div>Supports Sites: <span data-bind="text:supportsSites"></span></div>

<div>Supports Photo Points: <span data-bind="text:supportsPhotoPoints"></span></div>


<div>Form sections: <ul data-bind="foreach:sections">
    <li><span data-bind="text:name"></span><span data-bind="visible:optional">(optional)</span></li>
</ul></div>
<button data-bind="click:$root.removeActivity" type="button" class="btn btn-mini pull-right">Remove</button>
<button data-bind="click:edit" type="button" class="btn btn-mini pull-right">Edit</button>
</script>

<script id="editActivityTmpl" type="text/html">

<form class="form-horizontal">
    <div class="control-group">
        <label class="control-label">Name: </label>
        <div class="controls">
            <input type="text" class="input-xxlarge" data-bind="value:name"></div>
        </div>
    </div>
    <div class="control-group">
        <label class="control-label">Type: </label>
        <div class="controls">
            <select data-bind="options:['Activity','Assessment','Report','Milestone'],value:type"></select>
        </div>
    </div>

    <div class="control-group">
        <label class="control-label">Category: </label>
        <div class="controls">
            <input type="text" class="input-xxlarge " data-bind="value:category">
        </div>
    </div>
    <div class="control-group">
        <label class="control-label">GMS ID: </label>
        <div class="controls">
            <input type="text" class="input-xxlarge" data-bind="value:gmsId">
        </div>
    </div>
    <div class="control-group">
        <label class="control-label">Supports sites?: </label>
        <div class="controls">
            <input type="checkbox" data-bind="checked:supportsSites">
        </div>
    </div>
    <div class="control-group">
        <label class="control-label">Supports photo points?: </label>
        <div class="controls">
            <input type="checkbox" data-bind="checked:supportsPhotoPoints">
        </div>
    </div>
    <div class="control-group">
        <label class="control-label">Min. optional sections: </label>
        <div class="controls">
            <input type="text" class="input-small" data-bind="value:minOptionalSectionsCompleted">
        </div>
    </div>
</form>

<div>Form sections: <ul data-bind="sortable:{data:sections}" class="output-drop-target sortableList small">
    <li>
        <form class="form-horizontal">
            <span class="pull-right"><i data-bind="click:$parent.removeFormSection" class="icon-remove"></i></span>
            <div class="control-group">
                <label class="control-label">Name: </label>
                <div class="controls">
                    <input type="text" class="input-xxlarge" data-bind="value:name">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label">Optional: </label>
                <div class="controls">
                    <input type="checkbox" data-bind="checked:optional">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label">Collapsed by default: </label>
                <div class="controls">
                    <input type="checkbox" data-bind="checked:collapsedByDefault">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label">Question text if optional: </label>
                <div class="controls">
                    <input type="text" class="input-xxlarge" data-bind="value:optionalQuestionText, disable:!optional()">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label">Template name (backwards compatibility only): </label>
                <div class="controls">
                    <input type="text" class="input-xxlarge" data-bind="value:templateName">
                </div>
            </div>
        </form>
    </li>
</ul></div>
<button data-bind="click:done" type="button" class="btn btn-mini pull-right">Done</button>
<button data-bind="click:addSection" type="button" class="btn btn-mini pull-right">Add</button>

</script>

<script id="viewOutputTmpl" type="text/html">
<div>Name: <span data-bind="text:name"></span></div>

<div>Title: <span data-bind="text:title"></span></div>

<div>Template: <span data-bind="text:template"></span></div>

<button data-bind="click:edit" type="button" class="btn btn-mini pull-right">Edit</button>
</script>

<script id="editOutputTmpl" type="text/html">

<div style="margin-top:4px"><span class="span3">Name:</span> <input type="text" class="input pull-right"
                                                                    data-bind="value:name"></div>

<div style="margin-top:4px"><span class="span3">Title:</span> <input type="text" class="input pull-right"
                                                                     data-bind="value:title"></div>

<div class="clearfix"><span class="span3">Template:</span> <input type="text" class="input pull-right"
                                                                  data-bind="value:template"></div>
</div>
<button data-bind="click:done" type="button" class="btn btn-mini pull-right">Done</button>
</script>

<asset:script>
    $(function(){

        var forms = JSON.parse('${(availableActivities as grails.converters.JSON).toString()}');
        var service = new ActivityFormService(fcConfig);
        var viewModel = new ActivityModelViewModel(forms, service);
        ko.applyBindings(viewModel);
        $('.form-selection select').select2();
    });
</asset:script>
</body>
</html>