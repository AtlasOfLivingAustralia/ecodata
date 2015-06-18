class UrlMappings {

	static mappings = {

        "/ws/submit/record"(controller: "mobile") { action = [POST: "submitRecord"] }
        "/ws/submit/recordMultiPart"(controller: "mobile"){ action = [POST:"submitRecordMultipart"] }
        "/ws/mobile/submitRecord"(controller: "mobile"){ action = [POST:"submitRecord"] }
        "/ws/mobile/submitRecordMultipart"(controller: "mobile"){ action = [POST:"submitRecordMultipart"] }
        "/ws/record/csv"(controller: "record"){ action = [GET:"csv"] }
        "/ws/record/uncertainIdentifications"(controller: "record"){ action = [GET:"listUncertainIdentifications"] }
        "/ws/record/"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/ws/record"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/ws/record/count"(controller: "record"){ action = [GET:"count"] }
        "/ws/record/user/$userId"(controller: "record", action: "listForUser")
        "/ws/record/images"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/ws/record/images/"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/ws/record/$id"(controller: "record"){ action = [GET:"getById", PUT:"updateById", DELETE:"deleteById", POST:"updateById"] }
        "/ws/images"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/ws/images/"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/ws/images/update"(controller: "record"){ action = [POST:"updateImages"] }
        "/ws/location/fix"(controller: "location"){ action = [GET:"fixupLocations"] }
        "/ws/location"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/ws/location/"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/ws/location/user/$userId"(controller: "location"){ action = [GET:"listForUser", DELETE: "deleteAllForUser"] }
        "/ws/location/$id"(controller: "location"){ action = [GET:"getById", PUT:"updateById", DELETE:"deleteById", POST:"updateById"] }

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
        "/ws/documentation/$version/$action/$id?" {
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
