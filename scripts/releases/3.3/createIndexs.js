db.user.createIndex( { "userId": 1 }, { unique: true } );
db.document.createIndex({"filename":1, "filepath":1, "status": 1} );
