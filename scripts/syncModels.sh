#!/bin/sh
cd /data/temp
rm -rf datamodels
mkdir datamodels

ssh ecodata-test.ala.org.au 'cd /data/ecodata/; tar --exclude-vcs -cf models.tar models'
scp ecodata-test.ala.org.au:/data/ecodata/models.tar ./datamodels
cd datamodels

tar --include "*dataModel.json" -x -f models.tar
tar --include "*activities-model.json" -x -f models.tar
tar --include "*programs-model.json" -x -f models.tar
cp -R models /data/ecodata/

tar -cf clean-models.tar models

scp clean-models.tar biocollect-dev.ala.org.au:/home/god08d

