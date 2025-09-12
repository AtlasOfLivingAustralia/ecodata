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
projectActivityIdRegex=".*\"projectActivityId\" : \"([^\"]+).*"
documentPathRegex=".*\"filepath\" : \"([0-9-]+).*"
documentFilenameRegex=".*\"filename\" : \"([^\"]+).*"

mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection project --query "{\"projectId\":\"$PROJECT_ID\"}" > project.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection site --query "{\"projects\":\"$PROJECT_ID\"}" > site.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection activity --query "{\"projectId\":\"$PROJECT_ID\"}" > activity.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection document --query "{\"projectId\":\"$PROJECT_ID\"}" > document.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection userPermission --query "{\"entityId\":\"$PROJECT_ID\"}" > userPermission.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection projectActivity --query "{\"projectId\":\"$PROJECT_ID\"}" > projectActivity.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection record --query "{\"projectId\":\"$PROJECT_ID\"}" > record.json
mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection report --query "{\"projectId\":\"$PROJECT_ID\"}" > report.json


if [ -f output.json ];
then
    rm output.json
fi

touch output.json

while read activity; do
   [[ $activity =~ $activityRegex ]]
   mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection output --query "{\"activityId\":\"${BASH_REMATCH[1]}\"}" >> output.json
done <activity.json

while read projectActivity; do
   [[ $projectActivity =~ $projectActivityIdRegex ]]
   echo ${BASH_REMATCH[1]}
   mongoexport -u ecodata -p $MONGO_PASSWORD --db $DB --collection document --query "{\"projectActivityId\":\"${BASH_REMATCH[1]}\", role:\"logo\"}" >> document.json
done <projectActivity.json

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


tar -cvzf ${PROJECT_ID}.tar.gz project.json site.json activity.json document.json userPermission.json projectActivity.json output.json record.json report.json documents
rm project.json
rm site.json
rm activity.json
rm document.json
rm userPermission.json
rm output.json
rm projectActivity.json
rm record.json
rm report.json

rm -r ./documents

