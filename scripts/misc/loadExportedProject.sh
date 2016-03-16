DATABASE_NAME=ecodata
DOCUMENT_BASE_PATH=/data/ecodata/uploads
#UPSERT=--upsert
UPSERT=

mkdir tmp
tar -xf $1 -C tmp/
cd tmp

mongoimport --db $DATABASE_NAME $UPSERT --collection project --file project.json
mongoimport --db $DATABASE_NAME $UPSERT --collection site --file site.json
mongoimport --db $DATABASE_NAME $UPSERT --collection activity --file activity.json
mongoimport --db $DATABASE_NAME $UPSERT --collection output --file output.json
mongoimport --db $DATABASE_NAME $UPSERT --collection document --file document.json
mongoimport --db $DATABASE_NAME $UPSERT --collection userPermission --file userPermission.json
mongoimport --db $DATABASE_NAME $UPSERT --collection projectActivity --file projectActivity.json
mongoimport --db $DATABASE_NAME $UPSERT --collection record --file record.json

cp -R documents/* $DOCUMENT_BASE_PATH

cd ..
rm -r tmp