#!/bin/bash
mongo -u ecodata -p "$1" ecodata addHubIdsToEntities.js
mongo -u ecodata -p "$1" ecodata addIndexToUserPermission.js
mongo -u ecodata -p "$1" ecodata createIndexs.js
mongo -u ecodata -p "$1" ecodata populateUserLogin.js
mongo -u ecodata -p "$1" ecodata updateHubImages.js
