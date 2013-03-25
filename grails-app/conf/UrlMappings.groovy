class UrlMappings {

	static mappings = {

        "/ws/$controller/$id?" {
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

		"/$controller/$action?/$id?" {
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
