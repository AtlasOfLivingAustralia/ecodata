#!/bin/bash

if [ -z "$1" ]
  then
    echo "Usage: exportOrganisation.sh <organisationId>"
    exit 1
fi
ORGANISATION_ID=$1
DB=ecodata

mongoexport --db $DB --collection organisation --query "{organisationId:'$ORGANISATION_ID'}" > organisation.json
mongoexport --db $DB --collection document --query "{organisationId:'$ORGANISATION_ID'}" > document.json
mongoexport --db $DB --collection userPermission --query "{organisationId:'$ORGANISATION_ID'}" > userPermission.json

tar -cvzf ${ORGANISATION_ID}.tar.gz organisation.json document.json userPermission.json
rm organisation.json
rm document.json
rm userPermission.json

