/*
 * Copyright (C) 2017 Atlas of Living Australia
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
 * Created by Temi on 16/10/17.
 */
var hubs = db.hub.find({});
print("updating " +hubs.count()+ " hubs");

while (hubs.hasNext()) {
    var hub = hubs.next();
    db.hub.update({hubId: hub.hubId}, {"$set": {"pages.projectFinder.facets": hub.facets}, "$unset": {facets: null}});
}

print("completed updating hubs");