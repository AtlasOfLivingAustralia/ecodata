self.data.siteCode = ko.observable().extend({metadata:{metadata:self.dataModel['siteCode'], context:self.$context, config:config}});
self.data.location.subscribe(function (obj) {
    var siteName =  $('#siteLocation').find(":selected").text().trim();
    if(obj && siteName != 'Select a location'){
        self.data.siteCode(siteName);
    }
    else if(siteName == 'Select a location'){
        self.data.siteCode("");
    }
});