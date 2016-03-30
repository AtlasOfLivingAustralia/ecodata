/**
 * Projects cannot be created with termsOfUseAccepted:false.
 *
 * This corrects these invalid termsOfUseAccepted values.
 */
db.project.update({termsOfUseAccepted:false},{$set:{termsOfUseAccepted:true}},{multi:true})