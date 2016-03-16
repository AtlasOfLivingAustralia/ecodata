DATABASE_NAME=ecodata
DOCUMENT_BASE_PATH=/data/ecodata/uploads
mkdir tmp
tar -xf $1 -C tmp/
cd tmp

mongoimport --db $DATABASE_NAME --upsert --collection organisation --file organisation.json
mongoimport --db $DATABASE_NAME --upsert --collection document --file document.json
mongoimport --db $DATABASE_NAME --upsert --collection userPermission --file userPermission.json

cp -R documents/* $DOCUMENT_BASE_PATH

cd ..
rm -r tmp