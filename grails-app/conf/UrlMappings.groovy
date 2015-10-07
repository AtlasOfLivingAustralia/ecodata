class UrlMappings {

	static mappings = {


        "/ws/record"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/ws/record/"(controller: "record"){ action = [GET:"list", POST:"create"] }

        "/ws/record/csv"(controller: "record"){ action = [GET:"csv"] }
        "/ws/record/uncertainIdentifications"(controller: "record"){ action = [GET:"listUncertainIdentifications"] }
        "/ws/record/count"(controller: "record"){ action = [GET:"count"] }
        "/ws/record/user/$userId"(controller: "record", action: "listForUser")
        "/ws/record/images"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/ws/record/images/"(controller: "record"){ action = [GET:"listRecordWithImages"] }

        "/ws/record/$id"(controller: "record"){ action = [GET:"get", PUT:"update", DELETE:"delete", POST:"update"] }

        "/ws/location"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/ws/location/"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/ws/location/user/$userId"(controller: "location"){ action = [GET:"listForUser", DELETE: "deleteAllForUser"] }
        "/ws/location/$id"(controller: "location"){ action = [GET:"get", PUT:"update", DELETE:"delete", POST:"update"] }

        "/ws/comment"(controller: "comment"){ action = [GET: 'list', POST: 'create'] }
        "/ws/comment/$id"(controller: "comment"){ action = [GET:"get", PUT:"update", DELETE:"delete", POST:"update"] }

        "/ws/activitiesForProject/$id" {
            controller = 'activity'
            action = 'activitiesForProject'
        }

		"/ws/project/promoted" {
			controller = 'project'
			action = 'promoted'
		}

        "/ws/assessment/$id?" {
            controller = 'activity'
            type = 'assessment'
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/ws/$controller/$id?(.$format)?" {
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

        "/ws/site/$id/poi" {
            controller = 'site'
            action = [POST:'createPoi']
        }

        "/ws/$controller/search" {
            action = [POST:"search"]
        }

		"/ws/$controller/$action?/$id?(.$format)?" {
		}

		"/$controller/$action?/$id?(.$format)?" {
		}

        "/ws/documentation/$version" {
            controller = 'documentation'
            action = 'index'
        }
        "/ws/documentation/$version/$action/$id?(.$format)?" {
            controller = 'documentation'
        }

        "/ws/external/$version/$action" {
            controller = 'external'
        }

        "/ws/$entity/$id/documents" {
            controller = 'document'
            action = 'find'
        }

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
