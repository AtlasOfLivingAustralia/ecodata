db.userPermission.update({version:{$exists:true}},{$unset:{version:true}}, {multi:true});
