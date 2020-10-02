var program = db.program.find();
print(program.count());

program.forEach(function (program){
    if (program.config === typeof undefined){
        print("========== updating program config from undefined to null: " + program.programId)
        db.program.update({$set: {config: null}})
    }else{
        print("No update required for this program: " + program.programId)
    }
})
