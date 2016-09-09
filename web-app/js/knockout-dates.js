/*
Handles the display and editing of UTC dates.

Declares a Knockout extender that allows UTC ISODates to be displayed and edited as simple dates in the form
 dd-MM-yyyy and with local timezone adjustment. Hours and minutes can optionally be shown and edited.

Declares a custom binding that allows dates to be changed using the Bootstrap datepicker
 (https://github.com/eternicode/bootstrap-datepicker).

The date values in the ViewModel are maintained as UTC dates as strings in ISO format (ISO8601 without milliseconds).

The extender adds a 'formattedDate' property to the observable. It is this property that should be bound
 to an element, eg

    <input data-bind="value: myDate.formattedDate" type=...../> or
    <span data-bind="text: myDate.formattedDate" />

The date is defined in the view model like this:

    self.myDate = ko.observable("${myDate}").extend({simpleDate: false});

The boolean indicates whether to show the time as well.

The extender also adds a 'date' property to the observable that holds the value as a Javascript date object.
This is used by the datepicker custom binding.

The custom binding listens for changes via the datepicker as well as direct edits to the input field and
 updates the model. It also updates the datepicker on change to the model.

*/

(function(){

    // creates an ISO8601 date string but without millis - to match the format used by the java thingy for BSON dates
    Date.prototype.toISOStringNoMillis = function() {
        function pad(n) { return n < 10 ? '0' + n : n }
        return this.getUTCFullYear() + '-'
            + pad(this.getUTCMonth() + 1) + '-'
            + pad(this.getUTCDate()) + 'T'
            + pad(this.getUTCHours()) + ':'
            + pad(this.getUTCMinutes()) + ':'
            + pad(this.getUTCSeconds()) + 'Z';
    };

    // Use native ISO date parsing or shim for old browsers (IE8)
    var D= new Date('2011-06-02T09:34:29+02:00');
    if(!D || +D!== 1307000069000){
        Date.fromISO= function(s){
            var day, tz,
                rx=/^(\d{4}\-\d\d\-\d\d([tT ][\d:\.]*)?)([zZ]|([+\-])(\d\d):(\d\d))?$/,
                p= rx.exec(s) || [];
            if(p[1]){
                day= p[1].split(/\D/);
                for(var i= 0, L= day.length; i<L; i++){
                    day[i]= parseInt(day[i], 10) || 0;
                }
                day[1]-= 1;
                day= new Date(Date.UTC.apply(Date, day));
                if(!day.getDate()) return NaN;
                if(p[5]){
                    tz= (parseInt(p[5], 10)*60);
                    if(p[6]) tz+= parseInt(p[6], 10);
                    if(p[4]== '+') tz*= -1;
                    if(tz) day.setUTCMinutes(day.getUTCMinutes()+ tz);
                }
                return day;
            }
            return NaN;
        }
    }
    else{
        Date.fromISO= function(s){
            return new Date(s);
        }
    }
})();

function isValidDate(d) {
    if ( Object.prototype.toString.call(d) !== "[object Date]" )
        return false;
    return !isNaN(d.getTime());
}

function convertToSimpleDate(isoDate, includeTime) {
    if (!isoDate) { return ''}
    var date = isoDate, strDate;
    if (typeof isoDate === 'string') {
        date = Date.fromISO(isoDate);
    }
    if (!isValidDate(date)) { return '' }
    strDate = pad(date.getDate(),2) + '-' + pad(date.getMonth() + 1,2) + '-' + date.getFullYear();
    strDate = pad(date.getDate(),2) + '-' + pad(date.getMonth() + 1,2) + '-' + date.getFullYear();
    if (includeTime) {
        strDate = strDate + ' ' + pad(date.getHours(),2) + ':' + pad(date.getMinutes(),2);
    }
    return strDate;
}

function convertToIsoDate(date) {
    if (typeof date === 'string') {
        if (date.length === 20 && date.charAt(19) === 'Z') {
            // already an ISO date string
            return date;
        } else if (date.length > 9){
            // assume a short date of the form dd-mm-yyyy
            var year = date.substr(6,4),
                month = Number(date.substr(3,2))- 1,
                day = date.substr(0,2),
                hours = date.length > 12 ? date.substr(11,2) : 0,
                minutes = date.length > 15 ? date.substr(14,2) : 0;
            var dt = new Date(year, month, day, hours, minutes);
            if (isValidDate(dt)) {
                return dt.toISOStringNoMillis();
            }
            else {
                return '';
            }
        } else {
            return '';
        }
    } else if (typeof date === 'object') {
        // assume a date object
        if (isValidDate(date)) {
            return date.toISOStringNoMillis();
        }
        else {
            return '';
        }

    } else {
        return '';
    }
}

function stringToDate(date) {
    if (typeof date === 'string') {
        if (date.length === 20 && date.charAt(19) === 'Z') {
            // already an ISO date string
            return Date.fromISO(date);
        } else if (date.length > 9){
            // assume a short date of the form dd-mm-yyyy
            var year = date.substr(6,4),
                month = Number(date.substr(3,2))- 1,
                day = date.substr(0,2),
                hours = date.length > 12 ? date.substr(11,2) : 0,
                minutes = date.length > 15 ? date.substr(14,2) : 0;
            return new Date(year, month, day, hours, minutes);
        } else {
            return undefined;
        }
    } else if (typeof date === 'object') {
        // assume a date object
        return date;
    } else {
        return undefined;
    }
}

(function() {

    // Binding to exclude the contained html from the current binding context.
    // Used when you want to bind a section of html to a different viewModel.
    ko.bindingHandlers.stopBinding = {
        init: function() {
            return { controlsDescendantBindings: true };
        }
    };
    ko.virtualElements.allowedBindings.stopBinding = true;

    // This extends an observable that holds a UTC ISODate. It creates properties that hold:
    //  a JS Date object - useful with datepicker; and
    //  a simple formatted date of the form dd-mm-yyyy useful for display.
    // The formatted date will include hh:MM if the includeTime argument is true
    ko.extenders.simpleDate = function (target, includeTime) {
        target.date = ko.computed({
            read: function () {
                return Date.fromISO(target());
            },

            write: function (newValue) {
                if (newValue) {
                    var current = target(),
                        valueToWrite = convertToIsoDate(newValue);

                    if (valueToWrite !== current) {
                        target(valueToWrite);
                    }
                } else {
                    // date has been cleared
                    target("");
                }
            }
        });
        target.formattedDate = ko.computed({
            read: function () {
                return convertToSimpleDate(target(), includeTime);
            },

            write: function (newValue) {
                if (newValue) {
                    var current = target(),
                        valueToWrite = convertToIsoDate(newValue);

                    if (valueToWrite !== current) {
                        target(valueToWrite);
                    }
                }
            }
        });

        target.date(target());
        target.formattedDate(target());

        return target;
    };

    /* Custom binding for Bootstrap datepicker */
    // This binds an element and a model observable to the bootstrap datepicker.
    // The element can be an input or container such as span, div, td.
    // The datepicker is 2-way bound to the model. An input element will be updated automatically,
    //  other elements may need an explicit text binding to the formatted model date (see
    //  clickToPickDate for an example of a simple element).
    ko.bindingHandlers.datepicker = {
        init: function(element, valueAccessor, allBindingsAccessor) {
            // set current date into the element
            var $element = $(element),
                initialDate = ko.utils.unwrapObservable(valueAccessor()),
                initialDateStr = convertToSimpleDate(initialDate);
            if ($element.is('input')) {
                $element.val(initialDateStr);
            } else {
                $element.data('date', initialDateStr);
            }

            //initialize datepicker with some optional options
            $element.datepicker({format: 'dd-mm-yyyy', autoclose: true});

            // if the parent container holds any element with the class 'open-datepicker'
            // then add a hook to do so
            $element.parent().find('.open-datepicker').click(function () {
                $element.datepicker('show');
            });

            var changeHandler = function(event) {
                var value = valueAccessor();
                if (ko.isObservable(value)) {
                    value(event.date);
                }
            };

            //when a user changes the date via the datepicker, update the view model
            ko.utils.registerEventHandler(element, "changeDate", changeHandler);
            ko.utils.registerEventHandler(element, "hide", changeHandler);

            //when a user changes the date via the input, update the view model
            ko.utils.registerEventHandler(element, "change", function() {
                var value = valueAccessor();
                if (ko.isObservable(value)) {
                    value(stringToDate(element.value));
                    $(element).trigger('blur');  // This is to trigger revalidation of the date field to remove existing validation errors.
                }
            });
        },
        update: function(element, valueAccessor)   {
            var widget = $(element).data("datepicker");
            //when the view model is updated, update the widget
            if (widget) {
                var date = ko.utils.unwrapObservable(valueAccessor());
                widget.date = date;
                if (!isNaN(widget.date)) {
                    widget.setDate(widget.date);
                }
            }
        }
    };

}());

function pad(number, length){
    var str = "" + number
    while (str.length < length) {
        str = '0'+str
    }
    return str
}

//wrapper for an observable that protects value until committed
// CG - Changed the way the protected observable works from value doesn't change until commit to
// value changes as edits are made with rollback.  This was to enable cross field dependencies in a table
// row - using a temp variable meant observers were not notified of changes until commit.
ko.protectedObservable = function(initialValue) {
    //private variables
    var _current = ko.observable(initialValue);
    var _committed = initialValue;

    var result = ko.dependentObservable({
        read: _current,
        write: function(newValue) {
           _current(newValue);
        }
    });

    //commit the temporary value to our observable, if it is different
    result.commit = function() {
        _committed = _current();
    };

    //notify subscribers to update their value with the original
    result.reset = function() {
        _current(_committed);
    };

    return result;
};

// This binding allows dates to be displayed as simple text that can be clicked to access
//  a date picker for in-place editing.
// A user prompt appears if the model has no value. this can be customised.
// A calendar icon is added after the bound element as a visual indicator that the date can be edited.
// A computed 'hasChanged' property provides an observable isDirty flag for external save/revert mechanisms.
// The 'datepicker' binding is applied to the element to integrate the bootstrap datepicker.
// NOTE you can use the datepicker binding directly if you have an input as your predefined element.
ko.bindingHandlers.clickToPickDate = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel) {
        var observable = valueAccessor(),
            userPrompt = $(element).attr('data-prompt'),
            prompt = userPrompt || 'Click to edit',
            icon = $('<i class="icon-calendar open-datepicker" title="Click to change date"></i>');

        observable.originalValue = observable.date();
        observable.hasChanged = ko.computed(function () {
            //console.log("original: " + observable.originalValue + " current: " + observable.date());
            var original = observable.originalValue.getTime();
            var current = observable.date().getTime();
            return (original != current) && (!isNaN(original) || !isNaN(current));
        });

        $(element).parent().append(icon);

        ko.applyBindingsToNode(element, {
            text: ko.computed(function() {
                // todo: style default text as grey
                return ko.utils.unwrapObservable(observable) !== "" ? observable.formattedDate() : prompt;
            }),
            datepicker: observable.date
        });
    }
};

/*
This binding allows text values to be displayed as simple text that can be clicked to access
 an input control for in-place editing.
 */
ko.bindingHandlers.clickToEdit = {
    init: function(element, valueAccessor) {
        var observable = valueAccessor(),
            link = document.createElement("a"),
            input = document.createElement("input"),
            dblclick = $(element).attr('data-edit-on-dblclick'),
            userPrompt = $(element).attr('data-prompt'),
            prompt = userPrompt || (dblclick ? 'Double-click to edit' : 'Click to edit'),
            linkBindings;

        // add any classes specified for the link element
        $(link).addClass($(element).attr('data-link-class'));
        // add any classes specified for the input element
        $(input).addClass($(element).attr('data-input-class'));

        element.appendChild(link);
        element.appendChild(input);

        observable.editing = ko.observable(false);
        observable.stopEditing = function () {
            $(input).blur();
            observable.editing(false)
        };

        linkBindings = {
            text: ko.computed(function() {
                // todo: style default text as grey
                var value = ko.utils.unwrapObservable(observable);
                return value !== "" ? value : prompt;
            }),
            visible: ko.computed(function() {
                return !observable.editing();
            })
        };

        // bind to either the click or dblclick event
        if (dblclick) {
            linkBindings.event = { dblclick: observable.editing.bind(null, true) };
        } else {
            linkBindings.click = observable.editing.bind(null, true);
        }

        ko.applyBindingsToNode(link, linkBindings);

        ko.applyBindingsToNode(input, {
            value: observable,
            visible: observable.editing,
            hasfocus: observable.editing
        });

        // quit editing on enter key
        $(input).keydown(function(e) {
            if (e.which === 13) {
                observable.stopEditing();
            }
        });
    }
};

/*
This binding allows small non-negative integers in the model to be displayed as a number of ticks
 and edited by spinner buttons.
 */
ko.bindingHandlers.ticks = {
    init: function(element, valueAccessor) {
        var observable = valueAccessor(),
            $parent = $(element).parent(),
            $buttons,
            $widget = $('<div class="tick-controls btn-group btn-group-vertical"></div>');

        $parent.css('padding','4px');
        $widget.append($('<button class="up btn btn-mini"><i class="icon-chevron-up"></i></button>'));
        $widget.append($('<button class="down btn btn-mini"><i class="icon-chevron-down"></i></button>'));
        $parent.append($widget);
        $buttons = $parent.find('button');

        $buttons.hide();

        ko.utils.registerEventHandler($parent, "mouseover", function() {
            $buttons.show();
        });

        ko.utils.registerEventHandler($parent, "mouseout", function() {
            $buttons.hide();
        });

        ko.utils.registerEventHandler($buttons, "click", function() {
            var isUp = $(this).hasClass('up'),
                value = Number(observable());
            if (isNaN(value)) { value = 0; }

            if (isUp) {
                observable("" + (value + 1));
            } else {
                if (value > 0) {
                    observable("" + (value - 1));
                }
            }
            return false;
        });
    },
    update: function(element, valueAccessor) {
        var observable = valueAccessor(), value,
            tick = '<i class="icon-ok"></i>', ticks = "";
        if (observable) {
            value = Number(ko.utils.unwrapObservable(observable));
            if (isNaN(value)) {
                $(element).html("");
            } else {
                //$(element).html(value);
                $(element).empty();
                for (i=0; i < value; i++) {
                    ticks += tick;
                }
                $(element).html(ticks);
            }
        }
    }
};

// handles simple or deferred computed objects
// see activity/edit.gsp for an example of use
ko.extenders.async = function(computedDeferred, initialValue) {

    var plainObservable = ko.observable(initialValue), currentDeferred;
    plainObservable.inProgress = ko.observable(false);

    ko.computed(function() {
        if (currentDeferred) {
            currentDeferred.reject();
            currentDeferred = null;
        }

        var newDeferred = computedDeferred();
        if (newDeferred &&
            (typeof newDeferred.done == "function")) {

            // It's a deferred
            plainObservable.inProgress(true);

            // Create our own wrapper so we can reject
            currentDeferred = $.Deferred().done(function(data) {
                plainObservable.inProgress(false);
                plainObservable(data);
            });
            newDeferred.done(currentDeferred.resolve);
        } else {
            // A real value, so just publish it immediately
            plainObservable(newDeferred);
        }
    });

    return plainObservable;
};

ko.bindingHandlers.fileUploadNoImage = {
    init: function(element, options) {

        var defaults = {autoUpload:true, forceIframeTransport:true};
        var settings = {};
        $.extend(settings, defaults, options());
        $(element).fileupload(settings);
    }
}

// A handy binding to iterate over the properties of an object.
ko.bindingHandlers.foreachprop = {
    transformObject: function (obj) {
        var properties = [];
        for (var key in obj) {
            if (obj.hasOwnProperty(key)) {
                properties.push({ key: key, value: obj[key] });
            }
        }
        return properties;
    },
    init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        var value = ko.utils.unwrapObservable(valueAccessor()),
            properties = ko.bindingHandlers.foreachprop.transformObject(value);
        ko.applyBindingsToNode(element, { foreach: properties });
        return { controlsDescendantBindings: true };
    }
};

// Compares this column to the current sort parameters and displays the appropriate sort icons.
// If this is the column that the model is currently sorted by, then shows an up or down icon
//  depending on the current sort order.
// Usage example: <th data-bind="sortIcon:sortParamsObject,click:sortBy" data-column="type">Type</th>
// The sortIcon binding takes an object or observable that contains a 'by' property and an 'order' property.
// The data-column attr defines the model value that the column holds. This is compared to the
//  current sort by value to see if this is the active column.
ko.bindingHandlers.sortIcon = {
    update: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        var $element = $(element),
            name = $element.data('column'),
            $icon = $element.find('i'),
            className = "icon-blank",
            sortParams = ko.utils.unwrapObservable(valueAccessor());
        // see if this is the active sort column
        if (sortParams.by() === name) {
            // and if so, choose an icon based on sort order
            className = sortParams.order() === 'desc' ? 'icon-chevron-down' : 'icon-chevron-up';
        }
        // insert the icon markup if it doesn't exist
        if ($icon.length === 0) {
            $icon = $("<i class='icon-blank'></i>").appendTo($element);
        }
        // set the computed class
        $icon.removeClass('icon-chevron-down').removeClass('icon-chevron-up').removeClass('icon-blank').addClass(className);
    }
};

/*
 * Fused Autocomplete supports two versions of autocomplete (original autocomplete implementation by Jorn Zaefferer and jquery_ui)
 * Expects three parameters source, name and guid.
 * Ajax response lists needs name attribute.
 * Doco url: http://bassistance.de/jquery-plugins/jquery-plugin-autocomplete/
 * Note: Autocomplete implementation by Jorn Zaefferer is now been deprecated and its been migrated to jquery_ui.
 *
*/

ko.bindingHandlers.fusedAutocomplete = {

    init: function (element, params) {
        var params = params();
        var options = {};
        var url = ko.utils.unwrapObservable(params.source);
        options.source = function(request, response) {
            $(element).addClass("ac_loading");
            $.ajax({
                url: url,
                dataType:'json',
                data: {q:request.term},
                success: function(data) {
                    var items = $.map(data.autoCompleteList, function(item) {
                        return {
                            label:item.name,
                            value: item.name,
                            source: item
                        }
                    });
                    response(items);

                },
                error: function() {
                    items = [{label:"Error during species lookup", value:request.term, source: {listId:'error-unmatched', name: request.term}}];
                    response(items);
                },
                complete: function() {
                    $(element).removeClass("ac_loading");
                }
            });
        };
        options.select = function(event, ui) {
            var selectedItem = ui.item;
            params.name(selectedItem.source.name);
            params.guid(selectedItem.source.guid);
        };

        if(!$(element).autocomplete(options).data("ui-autocomplete")){
            // Fall back mechanism to handle deprecated version of autocomplete.
            var options = {};
            options.source = url;
            options.matchSubset = false;
            options.formatItem = function(row, i, n) {
                return row.name;
            };
            options.highlight = false;
            options.parse = function(data) {
                var rows = new Array();
                data = data.autoCompleteList;
                for(var i=0; i < data.length; i++) {
                    rows[i] = {
                        data: data[i],
                        value: data[i],
                        result: data[i].name
                    };
                }
                return rows;
            };

            $(element).autocomplete(options.source, options).result(function(event, data, formatted) {
                if (data) {
                    params.name(data.name);
                    params.guid(data.guid);
                }
            });
        }
    }
};

ko.bindingHandlers.autocomplete = {
    init: function (element, params) {
        var param = params();
        var url = ko.utils.unwrapObservable(param.url);
        var list = ko.utils.unwrapObservable(param.listId);
        var valueCallback = ko.utils.unwrapObservable(param.valueChangeCallback)
        var options = {};

        options.source = function(request, response) {
            $(element).addClass("ac_loading");

            if (valueCallback !== undefined) {
                valueCallback(request.term);
            }
            var data = {q:request.term};
            if (list) {
                $.extend(data, {druid: list});
            }
            $.ajax({
                url: url,
                dataType:'json',
                data: data,
                success: function(data) {
                    var items = $.map(data.autoCompleteList, function(item) {
                        return {
                            label:item.name,
                            value: item.name,
                            source: item
                        }
                    });
                    items = [{label:"Missing or unidentified species", value:request.term, source: {listId:'unmatched', name: request.term}}].concat(items);
                    response(items);

                },
                error: function() {
                    items = [{label:"Error during species lookup", value:request.term, source: {listId:'error-unmatched', name: request.term}}];
                    response(items);
                },
                complete: function() {
                    $(element).removeClass("ac_loading");
                }
            });
        };
        options.select = function(event, ui) {
            ko.utils.unwrapObservable(param.result)(event, ui.item.source);
        };

        var render = ko.utils.unwrapObservable(param.render);
        if (render && $(element).autocomplete(options).data("ui-autocomplete")) {

            $(element).autocomplete(options).data("ui-autocomplete")._renderItem = function(ul, item) {
                var result = $('<li></li>').html(render(item.source));
                return result.appendTo(ul);

            };
        }
        else {
            $(element).autocomplete(options);
        }
    }
};

/**
 * Creates a flag that indicates whether the model has been modified.
 *
 * Compares the model to its initial state each time an observable changes. Uses the model's
 * modelAsJSON method if it is defined else uses ko.toJSON.
 *
 * @param root the model to watch
 * @param isInitiallyDirty
 * @returns an object (function) with the methods 'isDirty' and 'reset'
 */
ko.dirtyFlag = function(root, isInitiallyDirty) {
    var result = function() {};
    var _isInitiallyDirty = ko.observable(isInitiallyDirty || false);
    // this allows for models that do not have a modelAsJSON method
    var getRepresentation = function () {
        return (typeof root.modelAsJSON === 'function') ? root.modelAsJSON() : ko.toJSON(root);
    };
    var _initialState = ko.observable(getRepresentation());

    result.isDirty = ko.dependentObservable(function() {
        var dirty = _isInitiallyDirty() || _initialState() !== getRepresentation();
        /*if (dirty) {
            console.log('Initial: ' + _initialState());
            console.log('Actual: ' + getRepresentation());
        }*/
        return dirty;
    });

    result.reset = function() {
        _initialState(getRepresentation());
        _isInitiallyDirty(false);
    };

    return result;
};

/**
 * A simple dirty flag that will detect the first change to a model, then afterwards always return true (meaning
 * dirty).  This is to prevent the full model being re-serialized to JSON on every change, which can cause
 * performance issues for large models.
 * From: http://www.knockmeout.net/2011/05/creating-smart-dirty-flag-in-knockoutjs.html
 * @param root the model.
 * @returns true if the model has changed since this function was added.
 */
ko.simpleDirtyFlag = function(root) {
    var _initialized = ko.observable(false);

    // this allows for models that do not have a modelAsJSON method
    var getRepresentation = function () {
        return (typeof root.modelAsJSON === 'function') ? root.modelAsJSON() : ko.toJSON(root);
    };

    var result = function() {};

    //one-time dirty flag that gives up its dependencies on first change
    result.isDirty = ko.computed(function () {
        if (!_initialized()) {

            //just for subscriptions
            getRepresentation();

            //next time return true and avoid ko.toJS
            _initialized(true);

            //on initialization this flag is not dirty
            return false;
        }

        //on subsequent changes, flag is now dirty
        return true;
    });
    result.reset = function() {
        _initialized(false);
    }

    return result;
};



/**
 * A vetoableObservable is an observable that provides a mechanism to prevent changes to its value under certain
 * conditions.  When a change is notified, the vetoCheck function is executed - if it returns false the change is
 * disallowed and the vetoCallback function is invoked.  Otherwise the change is allowed and the noVetoCallback
 * function is invoked.
 * The only current example of it's use is when the type of an activity is changed, it
 * can potentially invalidate any target score values that have been supplied by the user - hence the user is
 * asked if they wish to proceed, and if so, the targets can be removed.
 * @param initialValue the initial value for the observable.
 * @param vetoCheck a function or string that will be invoked when the value of the vetoableObservable changes.  Returning
 * false from this function will disallow the change.  If a string is supplied, it is used as the question text
 * for a window.confirm function.
 * @param noVetoCallback this callback will be invoked when a change to the vetoableObservable is allowed.
 * @param vetoCallback this callback will be invoked when a change to the vetoableObservable is disallowed (has been vetoed).
 * @returns {*}
 */
ko.vetoableObservable = function(initialValue, vetoCheck, noVetoCallback, vetoCallback) {
    //private variables
    var _current = ko.observable(initialValue);

    var vetoFunction = typeof (vetoCheck) === 'function' ? vetoCheck : function() {
        return window.confirm(vetoCheck);
    };
    var result = ko.dependentObservable({
        read: _current,
        write: function(newValue) {

            // The equality check is treating undefined as equal to an empty string to prevent
            // the initial population of the value with an empty select option from triggering the veto.
            if (_current() !== newValue && (_current() !== undefined || newValue !== '')) {

                if (vetoFunction()) {
                    _current(newValue);
                    if (noVetoCallback !== undefined) {
                        noVetoCallback();
                    }
                }
                else {
                    _current.notifySubscribers();
                    if (vetoCallback !== undefined) {
                        vetoCallback();
                    }
                }
            }

        }
    });

    return result;
};

/**
 *
 * @param target the knockoutjs object being extended.
 * @param options {currencySymbol, decimalSeparator, thousandsSeparator}
 */
ko.extenders.currency = function(target, options) {

    var symbol, d,t;
    if (options !== undefined) {
        symbol = options.currencySymbol;
        d = options.decimalSeparator;
        t = options.thousandsSeparator;
    }
    target.formattedCurrency = ko.computed(function() {
        var n = target(),
            c = isNaN(c = Math.abs(c)) ? 2 : c,
            d = d == undefined ? "." : d,
            t = t == undefined ? "," : t,
            s = n < 0 ? "-" : "",
            sym = symbol == undefined ? "$" : symbol,
            i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "",
            j = (j = i.length) > 3 ? j % 3 : 0;
        return sym + s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
    });
    return target;
};

// custom validator to ensure that only one of two fields is populated
function exclusive (field, rules, i, options) {
    var otherFieldId = rules[i+2], // get the id of the other field
        otherValue = $('#'+otherFieldId).val(),
        thisValue = field.val(),
        message = rules[i+3];
    // checking thisValue is technically redundant as this validator is only called
    // if there is a value in the field
    if (otherValue !== '' && thisValue !== '') {
        return message;
    } else {
        return true;
    }
};

/**
 * Converts markdown formatted text into html, filters an allowed list of tags.  (To prevent script injection).
 * @param target the knockout observable holding the text.
 * @param options unused.
 * @returns {*}
 */
ko.extenders.markdown = function(target, options) {
    var converter = new window.Showdown.converter();
    var filterOptions = window.WMDEditor.defaults.tagFilter;

    target.markdownToHtml = ko.computed(function() {
        var text = target();
        if (text) {
            text = text.replace(/<[^<>]*>?/gi, function (tag) {
                return (tag.match(filterOptions.allowedTags) || tag.match(filterOptions.patternLink) || tag.match(filterOptions.patternImage) || tag.match(filterOptions.patternAudio)) ? tag : "";
            });
        }
        else {
            text = '';
        }
        return converter.makeHtml(text);
    });
    return target;
};


ko.bindingHandlers.stagedImageUpload = {
    init: function(element, valueAccessor, allBindings, viewModel, bindingContext) {

        var defaultConfig = {
            maxWidth: 300,
            minWidth:150,
            minHeight:150,
            maxHeight: 300,
            previewSelector: '.preview'
        };
        var size = ko.observable();
        var progress = ko.observable();
        var error = ko.observable();
        var complete = ko.observable(true);

        var uploadProperties = {
            size: size,
            progress: progress,
            error:error,
            complete:complete
        };
        var innerContext = bindingContext.createChildContext(bindingContext);
        ko.utils.extend(innerContext, uploadProperties);

        var target = valueAccessor();
        var $elem = $(element);
        var role = $elem.data('role');
        var ownerKey = $elem.data('owner-type');
        var ownerValue = $elem.data('owner-id');
        var url = $elem.data('url');
        var owner = {};
        owner[ownerKey] = ownerValue;
        var config = {
            url:url,
            role: role,
            owner:owner
        };
        config = $.extend({}, defaultConfig, config);

         // Expected to be a ko.observableArray
        $(element).fileupload({
            url:config.url,
            autoUpload:true,
            forceIframeTransport: true
        }).on('fileuploadadd', function(e, data) {
            complete(false);
            progress(1);
        }).on('fileuploadprocessalways', function(e, data) {
            if (data.files[0].preview) {
                if (config.previewSelector !== undefined) {
                    var previewElem = $(element).parent().find(config.previewSelector);
                    previewElem.append(data.files[0].preview);
                }
            }
        }).on('fileuploadprogressall', function(e, data) {
            progress(Math.floor(data.loaded / data.total * 100));
            size(data.total);
        }).on('fileuploaddone', function(e, data) {

            var resultText = $('pre', data.result).text();
            var result = $.parseJSON(resultText);

            if (!result) {
                result = {};
                error('No response from server');
            }

            if (result.files[0]) {
                target.push(ko.bindingHandlers.stagedImageUpload.toDocument(result.files[0], config));
                complete(true);
            }
            else {
                error(result.error);
            }

        }).on('fileuploadfail', function(e, data) {
            error(data.errorThrown);
        });

        ko.applyBindingsToDescendants(innerContext, element);

        return { controlsDescendantBindings: true };
    },
    toDocument:function(f, config) {

        var data = {
            thumbnailUrl: f.thumbnail_url,
            url: f.url,
            contentType: f.contentType,
            filename: f.name,
            filesize: f.size,
            dateTaken: f.isoDate,
            lat: f.decimalLatitude,
            lng: f.decimalLongitude,
            name: f.name,
            type: 'image',
            role:config.role
        };

        return $.extend({}, data, config.owner);
    }
};

var ACTIVITY_PROGRESS_CLASSES = {
    'planned':'btn-warning',
    'started':'btn-success',
    'finished':'btn-info',
    'deferred':'btn-danger',
    'cancelled':'btn-inverse'
};


/** Returns a bootstrap class used to style activity progress labels */
function activityProgressClass(progress) {
    return ACTIVITY_PROGRESS_CLASSES[progress];
}

/** Allows a subscription to an observable that passes both the old and new value to the callback */
ko.subscribable.fn.subscribeChanged = function (callback) {
    var savedValue = this.peek();
    return this.subscribe(function (latestValue) {
        var oldValue = savedValue;
        savedValue = latestValue;
        callback(latestValue, oldValue);
    });
};

ko.extenders.numericString = function(target, precision) {
    //create a writable computed observable to intercept writes to our observable
    var result = ko.computed({
        read: target,  //always return the original observables value
        write: function(newValue) {
            var val = newValue;
            if (typeof val === 'string') {
                val = newValue.replace(/,|\$/g, '');
            }
            var current = target(),
                roundingMultiplier = Math.pow(10, precision),
                newValueAsNum = isNaN(val) ? 0 : parseFloat(+val),
                valueToWrite = Math.round(newValueAsNum * roundingMultiplier) / roundingMultiplier;

            //only write if it changed
            if (valueToWrite.toString() !== current || isNaN(val)) {
                target(isNaN(val) ? newValue : valueToWrite.toString());
            }
            else {
                if (newValue !== current) {
                    target.notifySubscribers(valueToWrite.toString());
                }
            }
        }
    }).extend({ notify: 'always' });

    //initialize with current value to make sure it is rounded appropriately
    result(target());

    //return the new computed observable
    return result;
};

ko.extenders.url = function(target) {
    var result = ko.pureComputed({
        read:target,
        write: function(url) {
            var value = typeof url == 'string' && url.indexOf("://") < 0? ("http://" + url): url;
            target(value);
        }
    });
    result(target());
    return result;
};

