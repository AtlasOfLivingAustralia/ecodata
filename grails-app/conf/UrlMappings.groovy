class UrlMappings {

	static mappings = {

        "/ws/assessment/$id?" {
            controller = 'activity'
            type = 'assessment'
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/ws/$controller/$id?" {
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

		"/$controller/$action?/$id?" {
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
