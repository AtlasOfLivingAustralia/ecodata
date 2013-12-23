class UrlMappings {

	static mappings = {

        "/ws/activitiesForProject/$id" {
            controller = 'activity'
            action = 'activitiesForProject'
        }

        "/ws/assessment/$id?" {
            controller = 'activity'
            type = 'assessment'
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/ws/$controller/$id?" {
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/ws/activities/" {
            controller = 'activity'
            action = [PUT:'bulkUpdate', POST:'bulkUpdate']
        }

        "/ws/metadata/$action/$id?" {
            controller = 'metadata'
        }

        "/ws/$entity/$id/raw" {
            controller = 'admin'
            action = 'getBare'
        }

		"/ws/$controller/$action?/$id?" {
		}

		"/$controller/$action?/$id?" {
		}

        "/ws/documentation/$version" {
            controller = 'documentation'
            action = 'index'
        }
        "/ws/documentation/$version/$action/$id?" {
            controller = 'documentation'
        }

        "/ws/external/$version/$action" {
            controller = 'external'
        }



		"/"(view:"/index")
		"500"(view:'/error')
	}
}
