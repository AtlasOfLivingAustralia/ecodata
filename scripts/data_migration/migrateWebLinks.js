var outputs = db.output.find({name:/Reef/});
while (outputs.hasNext()) {
    var output = outputs.next();

    if (output.data && output.data.actions) {
        for (var i=0; i<output.data.actions.length; i++) {
            var webLinks = output.data.actions[i].webLinks;

            if (webLinks) {
                print(webLinks + ' : ' + typeof(webLinks));
                if (typeof(webLinks.split) == 'function') {
                    print("splitting!");
                    var split = webLinks.split(/[\s|,]+/);
                    print(typeof(split));
                    print('('+split.join('),(')+')');

                    output.data.actions[i].webLinks = split;

                    db.output.save(output);
                }

            }


        }
    }

}