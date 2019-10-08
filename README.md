## Ecodata

### Build Status

[![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/ecodata.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/ecodata)

### About
Ecodata provides primarily data services for the [MERIT](https://github.com/AtlasOfLivingAustralia/fieldcapture) and [BioCollect](https://github.com/AtlasOfLivingAustralia/fieldcapture) applications.
See [MERIT schema](https://github.com/AtlasOfLivingAustralia/ecodata/wiki/MeritSchema) and 
[BioCollect schema](https://github.com/AtlasOfLivingAustralia/ecodata/wiki/BioCollectSchema) for an overview of the data.

It implements a JSON/HTTP API to provide access to the data.

### Technologies
* Grails framework 3.3.10
* MongoDB 4.0
* Elasticsearch 1.7
* Java 8

### Setup
* Clone the repository to your development machine.
* Create local directories: 
```
/data/ecodata/config
/data/ecodata/uploads
```
* Ecodata expects by default the program, activity and output configuration files to be found at:
```
/data/ecodata/models
```
These models are checked in to git in the $PROJECT_ROOT/models folder.  You can either create a soft link to this folder from /data/ecodata/models or change Config.groovy
```
environments {
    development {
        ...
        app.external.model.dir = "./models/"
        ...
```
* The application expects any external configuration file to be located in the path below.  An example configuration can be found at: https://github.com/AtlasOfLivingAustralia/ala-install/blob/master/ansible/inventories/vagrant/ecodata-vagrant
```
/data/ecodata/config/ecodata-config.properties
```
This configuration file largely specifies URLs to ecodata dependencies.  See https://github.com/AtlasOfLivingAustralia/ecodata/wiki/Ecodata-Dependencies for information about these.
Note that you will need to obtain an ALA API key to use ALA services and a Google Maps API key and specify them in this file.

### Testing
* To run the grails unit tests, use:
```
grails test-app
```

### Running

ecodata depends on a running instance of CAS and the API Key service so ensure these dependencies are available in your environment and configured correctly in ecodata-config.properties.
```
grails run-app 
```
Because of the embedded elasticsearch index and the creation of in-memory spreadsheets for download, ecodata can benefit from a larger heap.  If you encounter OutOfMemoryErrors, increase the application heap size with -Xmx4G
