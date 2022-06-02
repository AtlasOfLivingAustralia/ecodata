#!/bin/bash
mongo -u ecodata -p "$1" ecodata migrateExternalIds.js
mongo -u ecodata -p "$1" ecodata migrateReportStatusChangeCategories.js
