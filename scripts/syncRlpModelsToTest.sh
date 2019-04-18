#!/bin/sh

cd ../models
tar -cf rlpModels.tar ./rlp*/dataModel.json

scp rlpModels.tar god08d@fieldcapture-test.ala.org.au:/tmp
ssh -t god08d@fieldcapture-test.ala.org.au 'sudo mv /tmp/rlpModels.tar /data/ecodata/models; cd /data/ecodata/models; sudo tar -xf rlpModels.tar; sudo chown -R tomcat7:tomcat7 rlp*;'




