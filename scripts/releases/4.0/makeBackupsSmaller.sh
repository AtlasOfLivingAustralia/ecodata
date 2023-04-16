q#!/bin/bash -v
DIR=/data/backups/weekly/
cd $DIR
mkdir audit
mkdir processed
# Because we loop in date order, the most recent copy of the auditMessages will be kept
# which is OK because the auditMessage collection is insert only so the latest version is always a superset of
# previous versions
for file in $( ls -tr ecodata*.tgz ); do
    echo $file
    tar -xf $file;
    # Newer backups have a different path to older ones
    if [ -f "./data/backups/dump/ecodata/auditMessage.bson" ];
    then
      mv ./data/backups/dump/ecodata/auditMessage* ./audit
    fi
    if [ -f "./data/dump/ecodata/auditMessage.bson" ];
    then
      mv ./data/dump/ecodata/auditMessage* ./audit
    fi
    rm $file
    tar -zcf $file ./data;
    mv $file ./processed/
    rm -r ./data;
done