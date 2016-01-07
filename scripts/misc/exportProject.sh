#!/bin/bash

if [ -z "$1" ]
  then
    echo "Usage: exportProject.sh <projectId>"
    exit 1
fi
PROJECT_ID=$1
DOCUMENT_BASE_PATH=/data/ecodata/uploads
DB=ecodata
activityRegex=".*\"activityId\" : \"([a-z0-9-]+).*"
documentPathRegex=".*\"filepath\" : \"([0-9-]+).*"
documentFilenameRegex=".*\"filename\" : \"([^\"]+).*"

mongoexport --db $DB --collection project --query "{projectId:'$PROJECT_ID'}" > project.json
mongoexport --db $DB --collection site --query "{projects:'$PROJECT_ID'}" > site.json
mongoexport --db $DB --collection activity --query "{projectId:'$PROJECT_ID'}" > activity.json
mongoexport --db $DB --collection document --query "{projectId:'$PROJECT_ID'}" > document.json
mongoexport --db $DB --collection userPermission --query "{entityId:'$PROJECT_ID'}" > userPermission.json
mongoexport --db $DB --collection projectActivity --query "{projectId:'$PROJECT_ID'}" > projectActivity.json
mongoexport --db $DB --collection record --query "{projectId:'$PROJECT_ID'}" > record.json


if [ -f output.json ];
then
    rm output.json
fi

touch output.json

while read activity; do
   [[ $activity =~ $activityRegex ]]
   mongoexport -db $DB --collection output --query "{activityId:'${BASH_REMATCH[1]}'}" >> output.json
done <activity.json

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
    echo $SUBPATH

    FULLPATH=$DOCUMENT_BASE_PATH/$SUBPATH/$FILENAME

    mkdir -p ./documents/$SUBPATH

    cp "$FULLPATH" "./documents/$SUBPATH"
done <document.json


tar -cvzf ${PROJECT_ID}.tar.gz project.json site.json activity.json document.json userPermission.json projectActivity.json output.json record.json documents
rm project.json
rm site.json
rm activity.json
rm document.json
rm userPermission.json
rm output.json
rm projectActivity.json
rm record.json

rm -r ./documents

