<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Programs model | Admin</title>
    <script type="text/javascript">
        var fcConfig = {
            updateProgramsModelUrl:"${createLink(action: 'updateProgramsModel')}"
        };
    </script>
</head>

<body>
<content tag="pageTitle">Programs model</content>
<content tag="adminButtonBar">
    <button type="button" data-bind="click:save" class="btn btn-success">Save</button>
    <button type="button" data-bind="click:revert" class="btn">Cancel</button>
</content>
<div class="row-fluid span10">
    <p class="span12">These lists control the programs, sub-programs and themes that can be associated
    with projects and activities.</p>
    <p><b>Click</b> an item to select it and show its properties. <b>Double-click</b> to edit a name.
        <b>Drag</b> to rearrange the order of items.</p>
</div>
<form id="validation-container">
<div class="row-fluid">
    <div class="span4">
        <h2>Programs</h2>
        <ul data-bind="sortable:{data:programs}" class="sortableList programs">
            <li class="item" data-bind="css:{referenced:isSelected}">
                <div data-bind="click:select">
                    <span data-bind="clickToEdit:name" data-edit-on-dblclick="true" data-input-class="auto-width"></span>%{--<span data-bind="visible:!name()">new</span>--}%
                    <span class="pull-right" data-bind="visible:isSelected"><i data-bind="click:$parent.removeProgram" class="icon-remove"></i></span>
                </div>
                <div data-bind="visible:isSelected">
                    <hr/>
                    <div><label for="isMeritProgramme">Reports via MERIT <input id="isMeritProgramme" type="checkbox" data-bind="checked:isMeritProgramme"></label></div>
                    <div><label for="reportingPeriod">Reporting period (months) <input id="reportingPeriod" class="input-small" type="number" data-bind="enabled:isMeritProgramme, value:reportingPeriod"></label></div>
                    <div><label for="reportingPeriodAlignedToCalendar">Reporting period is aligned to calendar dates <input id="reportingPeriodAlignedToCalendar" type="checkbox" data-bind="enabled:isMeritProgramme, checked:reportingPeriodAlignedToCalendar"></label></div>
                    <div><label for="projectDatesContracted">Projects must start and end on contract dates <input id="projectDatesContracted" type="checkbox" data-bind="checked:projectDatesContracted"></label></div>
                    <div><label for="weekDaysToCompleteReport">Number of weekdays to after a stage ends after which the report is due <input id="weekDaysToCompleteReport" type="text" class="input-small" data-bind="value:weekDaysToCompleteReport" data-validation-engine="validate[number]"></label></div>

                    <div class="optional-project-content">
                        <label>Optional project content</label>
                        <ul class="unstyled" data-bind="foreach:{data: $root.transients.optionalProjectContent}">
                            <li class="text-left"><input type="checkbox" name="optionalProjectContent" data-bind="value:$data, checked:$parent.optionalProjectContent"> <span data-bind="text:$data"></span></li>
                        </ul>
                    </div>
                    <div><label data-bind="click:toggleActivities">Activities <span data-bind="text:'(' + activities().length + ' selected)'"></span></label></div>
                    <div class="program-activities" data-bind="visible:transients.showActivities">
                        <div data-bind="foreach:{data: $root.transients.activityTypes}">
                            <strong><span data-bind="text:name"></span></strong>
                            <ul class="unstyled" data-bind="foreach:list">
                                <li><input type="checkbox" name="activity" data-bind="value:name,attr:{id:'activity'+$index()},checked:$parents[1].activities" data-validation-engine="validate[minCheckbox[1]]"> <span data-bind="text:name"></span></li>
                            </ul>
                        </div>
                    </div>
                    <div>
                        <label data-bind="click:toggleSpeciesSettings">Activity species field settings</label>
                    </div>

                    <div data-bind="visible:transients.showSpeciesSettings">
                        <textarea class="species-settings" data-bind="value:speciesFieldsSettings"></textarea>
                    </div>
                </div>
            </li>
        </ul>
        <span data-bind="click:addProgram" class="clickable"><i class="icon-plus"></i> Add another</span>
    </div>
    <div class="span4">
        <h2>Sub-programs</h2>
        <ul data-bind="sortable:{data:transients.displayedSubprograms}" class="sortableList subprograms">
            <li class="item" data-bind="css:{referenced:isSelected}">
                <div data-bind="click:select">
                    <span data-bind="clickToEdit:name" data-edit-on-dblclick="true" data-input-class="auto-width"></span>
                    <span class="pull-right" data-bind="visible:isSelected"><i data-bind="click:$parent.removeSubprogram" class="icon-remove"></i></span>
                </div>
                <div>Start Date <ec:datePicker class="input-small" targetField="startDate.date" name="startDate"/></div>
                <div>End Date <ec:datePicker class="input-small" targetField="endDate.date" name="endDate"/></div>
                <div><label for="overridesProgramData">Override program configuration <input id="overridesProgramData" type="checkbox" data-bind="checked:overridesProgramData"></label></div>

                <div data-bind="visible:overridesProgramData">
                    <div><label for="subProgrammeReportingPeriod">Reporting period (months) <input id="subProgrammeReportingPeriod" class="input-small" type="number" data-bind="enabled:isMeritProgramme, value:reportingPeriod"></label></div>
                    <div><label for="subProgrammeReportingPeriodAlignedToCalendar">Reporting period is aligned to calendar dates <input id="subProgrammeReportingPeriodAlignedToCalendar" type="checkbox" data-bind="enabled:isMeritProgramme, checked:reportingPeriodAlignedToCalendar"></label></div>
                    <div><label for="subProgrammeProjectDatesContracted">Projects must start and end on contract dates <input id="subProgrammeProjectDatesContracted" type="checkbox" data-bind="checked:projectDatesContracted"></label></div>
                    <div><label for="weekDaysToCompleteReport">Number of weekdays to after a stage ends after which the report is due <input id="weekDaysToCompleteReport" type="text" class="input-small" data-bind="value:weekDaysToCompleteReport" data-validation-engine="validate[number]"></label></div>

                    <div class="optional-project-content">
                        <label>Optional project content</label>
                        <ul class="unstyled" data-bind="foreach:{data: $root.transients.optionalProjectContent}">
                            <li class="text-left"><input type="checkbox" name="optionalProjectContent" data-bind="value:$data, checked:$parent.optionalProjectContent"> <span data-bind="text:$data"></span></li>
                        </ul>
                    </div>
                    <div><label data-bind="click:toggleActivities">Activities <span data-bind="text:'(' + activities().length + ' selected)'"></span></label></div>
                    <div class="program-activities" data-bind="visible:transients.showActivities">
                        <div data-bind="foreach:{data: $root.transients.activityTypes}">
                            <strong><span data-bind="text:name"></span></strong>
                            <ul class="unstyled" data-bind="foreach:list">
                                <li><input type="checkbox" name="activity" data-bind="value:name,attr:{id:'activity'+$index()},checked:$parents[1].activities" data-validation-engine="validate[minCheckbox[1]]"> <span data-bind="text:name"></span></li>
                            </ul>
                        </div>
                    </div>
                    <div>
                        <label data-bind="click:toggleSpeciesSettings">Activity species field settings</label>
                    </div>

                    <div data-bind="visible:transients.showSpeciesSettings">
                        <textarea id="subprogram-species" class="species-settings" data-bind="value:speciesFieldsSettings"></textarea>
                    </div>
                </div>
            </li>
        </ul>
        <span data-bind="click:addSubprogram, visible:transients.selectedProgram()" class="clickable"><i class="icon-plus"></i> Add another</span>
    </div>
    <div class="span4">
        <h2>Themes</h2>
        <ul data-bind="sortable:{data:transients.displayedThemes}" class="sortableList themes">
            <li class="item" data-bind="css:{referenced:isSelected}">
                <div data-bind="click:select">
                    <span data-bind="clickToEdit:name" data-edit-on-dblclick="true" data-input-class="auto-width"></span>
                    <span class="pull-right" data-bind="visible:isSelected"><i data-bind="click:$parent.removeTheme" class="icon-remove"></i></span>
                </div>
            </li>
        </ul>
        <span data-bind="click:addTheme, visible:transients.selectedSubprogram()" class="clickable"><i class="icon-plus"></i> Add another</span>
    </div>
</div>

</form>

<asset:script type="text/javascript">
    $(function(){

        $('#validationContainer').validationEngine();

        var options = {
            updateProgramsModelUrl:fcConfig.updateProgramsModelUrl
        };
        var activityTypes = JSON.parse('${(activityTypes as grails.converters.JSON).toString().encodeAsJavaScript()}');
        var programsModel = JSON.parse('${(programsModel as grails.converters.JSON).toString().encodeAsJavaScript()}');
        var viewModel = new ProgramModelViewModel(programsModel, activityTypes, options);
        ko.applyBindings(viewModel);
    });
</asset:script>
</body>
</html>