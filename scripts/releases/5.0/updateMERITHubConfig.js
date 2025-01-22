db.hub.updateOne({urlPath: "merit"}, {$set: {geographicConfig: {
            "contextual": {
                "state" : 'cl927',
                "nrm" : 'cl11160',
                "lga" : 'cl959',
                "elect" : 'cl11163',
                "cmz" : 'cl2112'
            },
            "checkForBoundaryIntersectionInLayers" : [ "cl927", "cl11163" ]
        }}});