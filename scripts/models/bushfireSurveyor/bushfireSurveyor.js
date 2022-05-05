self.data.recordType = ko.observable().extend({metadata:{metadata:self.dataModel['recordType'], context:self.$context, config:config}});

$('div[data-bind*="propertyInformationRepeatSection"]').parent().parent().hide()
$('div[data-bind*="bushInformationRepeatSection"]').parent().parent().hide()

self.data.recordType.subscribe(function(obj) {
    var recordType =self.data.recordType();

    if(recordType == "Property (house and garden)"){
        $('div[data-bind*="propertyInformationRepeatSection"]').parent().parent().show()
        $('div[data-bind*="bushInformationRepeatSection"]').parent().parent().hide()
    }
    else if(recordType == "Bushland"){
        $('div[data-bind*="propertyInformationRepeatSection"]').parent().parent().hide()
        $('div[data-bind*="bushInformationRepeatSection"]').parent().parent().show()
    }
    else{
        $('div[data-bind*="propertyInformationRepeatSection"]').parent().parent().hide()
        $('div[data-bind*="bushInformationRepeatSection"]').parent().parent().hide()
    }
});