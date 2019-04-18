#!/bin/sh

rm -rf datamodels
mkdir datamodels

ssh god08d@ecodata.ala.org.au 'cd /data/ecodata/; tar --exclude-vcs -cf models.tar models/rlp*'
scp god08d@ecodata.ala.org.au:/data/ecodata/models.tar ./datamodels
cd datamodels

tar -x -f models.tar
cp -R models ../../


