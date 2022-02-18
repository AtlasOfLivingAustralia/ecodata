self.data.recordType = ko.observable().extend({metadata:{metadata:self.dataModel['recordType'], context:self.$context, config:config}});

$('[data-bind="click:data.propertyInformationRepeatSection.addRow"]').parent().hide()
$('[data-bind="click:data.bushInformationRepeatSection.addRow"]').parent().hide()

self.data.recordType.subscribe(function(obj) {
    var recordType =self.data.recordType();

    if(recordType == "Property (house and garden)"){
        $('[data-bind="click:data.propertyInformationRepeatSection.addRow"]').parent().show()
        $('[data-bind="click:data.bushInformationRepeatSection.addRow"]').parent().hide()
    }
    else if(recordType == "Bushland"){
        $('[data-bind="click:data.propertyInformationRepeatSection.addRow"]').parent().hide()
        $('[data-bind="click:data.bushInformationRepeatSection.addRow"]').parent().show()
    }
    else{
        $('[data-bind="click:data.propertyInformationRepeatSection.addRow"]').parent().hide()
        $('[data-bind="click:data.bushInformationRepeatSection.addRow"]').parent().hide()
    }
});