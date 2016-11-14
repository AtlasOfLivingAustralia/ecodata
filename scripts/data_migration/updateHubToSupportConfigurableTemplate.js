/*
 * Copyright (C) 2016 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Created by Temi on 14/11/16.
 */
var hubs = db.hub.find({urlPath: 'mdba'})
while (hubs.hasNext()) {
    var templateConfiguration = {
        "footer": undefined,
        "styles": undefined,
        "header": undefined,
        "homePage": undefined,
        "banner": {
            "images": [],
            "transitionSpeed": 4000
        }
    };

    var hub = hubs.next();
    if(!hub.templateConfiguration){
        if(!hub.bannerUrl){
            templateConfiguration.banner.images = [{url: hub.bannerUrl, caption: ''}];
        }

        db.hub.update({hubId:hub.hubId},{$set:{templateConfiguration:templateConfiguration}});
        print("Updated - " + hub.urlPath);
    }
}