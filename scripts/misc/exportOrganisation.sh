#!/bin/bash

if [ -z "$1" ]
  then
    echo "Usage: exportOrganisation.sh <organisationId>"
    exit 1
fi
ORGANISATION_ID=$1
DB=ecodata
DOCUMENT_BASE_PATH=/data/ecodata/uploads
documentPathRegex=".*\"filepath\" : \"([0-9-]+).*"
documentFilenameRegex=".*\"filename\" : \"([^\"]+).*"

mongoexport --db $DB --collection organisation --query "{organisationId:'$ORGANISATION_ID'}" > organisation.json
mongoexport --db $DB --collection document --query "{organisationId:'$ORGANISATION_ID'}" > document.json
mongoexport --db $DB --collection userPermission --query "{organisationId:'$ORGANISATION_ID'}" > userPermission.json


mkdir documents
while read document; do
    FILENAME=
    SUBPATH=

    if [[ $document =~ $documentFilenameRegex ]]; then
        FILENAME=${BASH_REMATCH[1]}
    fi
    if [[ $document =~ $documentPathRegex ]]; then
        SUBPATH=${BASH_REMATCH[1]}
    fi

    FULLPATH=$DOCUMENT_BASE_PATH/$SUBPATH/$FILENAME

    mkdir -p ./documents/$SUBPATH

    cp "$FULLPATH" "./documents/$SUBPATH"
done <document.json


tar -cvzf ${ORGANISATION_ID}.tar.gz organisation.json document.json userPermission.json documents
rm organisation.json
rm document.json
rm userPermission.json
rm -r ./documents

