var program = db.program.find({config:{$type: 'undefined'}});
print(program.count());

program.forEach(function (pgm){
        print("========== updating program config from undefined to null: " + pgm.programId)
         db.program.update({programId: pgm.programId}, {$set: {config: null}})
});
