DATABASE_NAME=ecodata
mkdir tmp
tar -xf $1 -C tmp/
cd tmp

mongoimport --db $DATABASE_NAME --upsert --collection project --file project.json
mongoimport --db $DATABASE_NAME --upsert --collection site --file site.json
mongoimport --db $DATABASE_NAME --upsert --collection activity --file activity.json
mongoimport --db $DATABASE_NAME --upsert --collection output --file output.json
mongoimport --db $DATABASE_NAME --upsert --collection document --file document.json
mongoimport --db $DATABASE_NAME --upsert --collection userPermission --file userPermission.json

cd ..
rm -r tmp