//= require jquery/jquery
//= require bootstrap/js/bootstrap
//= require knockout/knockout-latest
//= require vendor/jquery-ui/jquery-ui
//= require knockout-sortable/knockout-sortable
//= require knockout-mapping/knockout.mapping
//= require vendor/jquery-validation-engine/jquery.validationEngine
//= require knockout-dates
//= require_self

if (typeof jQuery !== 'undefined') {
	(function($) {
		$('#spinner').ajaxStart(function() {
			$(this).fadeIn();
		}).ajaxStop(function() {
			$(this).fadeOut();
		});
	})(jQuery);
}

/**
 * Show a modal dialog whose contents are sourced from a url specified in the options block
 *
 * This function injects a div into the current DOM, so no target element is required. The div is removed when the model is closed.
 *
 * @param options
 */
function showModal(options) {

    var opts = {
        url: options.url ? options.url : false,
        id: options.id ? options.id : 'modal_element_id',
        height: options.height ? options.height : 500,
        width: options.width ? options.width : 600,
        title: options.title ? options.title : 'Modal Title',
        hideHeader: options.hideHeader ? options.hideHeader : false,
        onClose: options.onClose ? options.onClose : null,
        onShown: options.onShown ? options.onShown : null
    }

    var html = "<div id='" + opts.id + "' class='modal hide fade' role='dialog' aria-labelledby='modal_label_" + opts.id + "' aria-hidden='true' style='width: " + opts.width + "px; margin-left: -" + opts.width / 2 + "px;overflow: hidden'>";
    if (!opts.hideHeader) {
        html += "<div class='modal-header'><button type='button' class='close' data-dismiss='modal' aria-hidden='true'>x</button><h3 id='modal_label_" + opts.id + "'>" + opts.title + "</h3></div>";
    }
    html += "<div class='modal-body' style='max-height: " + opts.height + "px'>Loading...</div></div>";

    $("body").append(html);

    var selector = "#" + opts.id;

    $(selector).on("hidden", function() {
        $(selector).remove();
        if (opts.onClose) {
            opts.onClose();
        }
    });

    $(selector).on("shown", function() {
        if (opts.onShown) {
            opts.onShown();
        }
    });

    $(selector).modal({
        remote: opts.url
    });
}

function hideModal() {
    $("#modal_element_id").modal('hide');
}

function setModalTitle(title) {
    $("#modal_label_modal_element_id").html(title);
}

