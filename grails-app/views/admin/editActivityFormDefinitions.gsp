<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Edit Activity Forms</title>
    <script>
        var fcConfig = {
            activityFormUpdateUrl: "${createLink(controller:'activityForm', action:'update')}",
            activityFormCreateUrl: "${createLink(controller:'activityForm', action:'create')}",
            getActivityFormUrl: "${createLink(action:'findActivityForm')}",
            reloadUrl: "${createLink(controller:'admin', action:'editActivityFormDefinitions')}",
            findUsersOfFormUrl: "${createLink(controller:'activityForm', action:'findUsesOfForm')}"
        };
    </script>

</head>

<body>
<content tag="pageTitle">Activity model</content>
<content tag="adminButtonBar">
    <button type="button" data-bind="click:save, enable:selectedActivity()" class="btn btn-success">Save</button>
    <button type="button" data-bind="click:revert" class="btn">Cancel</button>
</content>

<div class="form-selection" data-bind="with:selectionModel">
    <div class="row">
        <div class="alert alert-danger" style="display:none" data-bind="visible:warning()">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <strong>Warning!</strong> <span data-bind="text:warning"></span>
        </div>
    </div>

    <div class="row">
        <div class="col-md-10">
            <label for="formSelector">Activity form:</label><select id="formSelector" name="formSelector"
                                                                    class="form-control"
                                                                    data-bind="options:activityForms, optionsCaption:'Select a form to edit', optionsText:'name', value:selectedFormName"></select>
        </div>

        <div class="col-md-2">
            <label for="formVersion">Version:</label> <select id="formVersion" style="width:100%;" class="form-control"
                                                              name="versionSelector"
                                                              data-bind="options:activityFormVersions, value:selectedFormVersion"></select></label>
        </div>
    </div>

</div>

<div class="row">
    <div class="col-12">
        <ul data-bind="with:selectedActivity" class="activityList sortableList">

            <li class="item" data-bind="css:{disabled:!enabled()}">

                <div data-bind="click:toggle"><h3 data-bind="text:name"></h3></div>

                <div class="details clearfix">
                    <div data-bind="template: {name: displayMode}"></div>
                </div>
            </li>
        </ul>

        <div class="button-bar">
            <button class="btn btn-secondary btn-sm" data-bind="click:addActivity, enable:!selectedActivity()"
                    class="clickable"><i class="fa fa-plus"></i> Add new</button>

            <span class="upload-btn-wrapper">
                <button type="button" class="btn btn-secondary btn-sm" data-bind="enable:!selectedActivity()"><i
                        class="fa fa-upload"></i> Import</button>
                <input type="file" id="fileinput" accept="application/json" name="myfile"
                       data-bind="enable:!selectedActivity(), event: {change:importActivity}"/>
            </span>
            <button type="button" data-bind="click:exportActivity, enable:selectedActivity()"
                    class="btn btn-secondary btn-sm"><i class="fa fa-download"></i> Export</button>
        </div>

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

<div class="section-buttons d-flex justify-content-end">
    <button data-bind="click:$root.removeActivity" type="button"
            class="btn btn-outline-secondary btn-sm">Remove</button>
    <button data-bind="click:edit" type="button" class="btn btn-outline-secondary btn-sm">Edit</button>
</div>
</script>

<script id="editActivityTmpl" type="text/html">

<form class="form-horizontal">
    <div class="row">
        <label class="col-md-3 col-form-label">Name:</label>

        <div class="col-md-9">
            <input type="text" class="form-control" data-bind="value:name"></div>
    </div>
</div>
    <div class="row">
        <label class="col-md-3 col-form-label">Type:</label>

        <div class="col-md-9">
            <select class="form-select"
                    data-bind="options:['Activity','Assessment','Report','Milestone'],value:type"></select>
        </div>
    </div>

    <div class="row">
        <label class="col-md-3 col-form-label">Category:</label>

        <div class="col-md-9">
            <input type="text" class="form-control" data-bind="value:category">
        </div>
    </div>

    <div class="row">
        <label class="col-md-3 col-form-label">GMS ID:</label>

        <div class="col-md-9">
            <input type="text" class="form-control" data-bind="value:gmsId">
        </div>
    </div>

    <div class="row">
        <label class="col-md-3 col-form-label form-check-label">Supports sites?:</label>

        <div class="col-md-9">
            <input type="checkbox" class="form-check-input" data-bind="checked:supportsSites">
        </div>
    </div>

    <div class="row">
        <label class="col-md-3 col-form-label form-check-label">Supports photo points?:</label>

        <div class="col-md-9">
            <input type="checkbox" class="form-check-input" data-bind="checked:supportsPhotoPoints">
        </div>
    </div>

    <div class="row">
        <label class="col-md-3 col-form-label">Min. optional sections:</label>

        <div class="col-md-9">
            <input type="number" class="form-control" data-bind="value:minOptionalSectionsCompleted">
        </div>
    </div>
</form>

<div>
    Form sections:
    <ul data-bind="sortable:{data:sections}" class="output-drop-target sortableList small">
        <li>
            <div class="row">
                <form class="form-horizontal">
                    <span class="pull-right"><i data-bind="click:$parent.removeFormSection" class="fa fa-remove"></i>
                    </span>

                    <div class="row">
                        <label class="col-md-3 col-form-label">Name:</label>

                        <div class="col-md-9">
                            <input type="text" class="form-control" data-bind="value:name">
                        </div>
                    </div>

                    <div class="row">
                        <label class="col-md-3 col-form-label form-check-label">Optional:</label>

                        <div class="col-md-9">
                            <input type="checkbox" class="form-check-input" data-bind="checked:optional">
                        </div>
                    </div>

                    <div class="row">
                        <label class="col-md-3 col-form-label form-check-label">Collapsed by default:</label>

                        <div class="col-md-9">
                            <input type="checkbox" class="form-check-input" data-bind="checked:collapsedByDefault">
                        </div>
                    </div>

                    <div class="row">
                        <label class="col-md-3 col-form-label">Question text if optional:</label>

                        <div class="col-md-9">
                            <input type="text" class="form-control"
                                   data-bind="value:optionalQuestionText, disable:!optional()">
                        </div>
                    </div>

                    <div class="row">
                        <label class="col-md-3 col-form-label">Template name (backwards compatibility only - MUST BE UNIQUE):</label>

                        <div class="col-md-9">
                            <input type="text" class="form-control" data-bind="value:templateName">
                        </div>
                    </div>
                </form>
            </div>
        </li>
    </ul>
</div>
<div class="section-buttons d-flex justify-content-end">
    <button data-bind="click:done" type="button" class="btn btn-secondary btn-sm">Done</button>
    <button data-bind="click:addSection" type="button" class="btn btn-secondary btn-sm">Add</button>
</div>
</script>


<asset:script>
    $(function(){

        var forms = JSON.parse('${raw((availableActivities as grails.converters.JSON).toString())}');
        var service = new ActivityFormService(fcConfig);
        var selectedForm = "${params.form}";
        var viewModel = new ActivityModelViewModel(forms, selectedForm, service, fcConfig);
        ko.applyBindings(viewModel);
        $('.form-selection select').select2();
    });
</asset:script>
</body>
</html>