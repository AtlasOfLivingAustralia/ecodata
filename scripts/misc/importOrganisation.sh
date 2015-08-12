DATABASE_NAME=ecodata
mkdir tmp
tar -xf $1 -C tmp/
cd tmp

mongoimport --db $DATABASE_NAME --upsert --collection organisation --file organisation.json
mongoimport --db $DATABASE_NAME --upsert --collection document --file document.json
mongoimport --db $DATABASE_NAME --upsert --collection userPermission --file userPermission.json

cd ..
rm -r tmp