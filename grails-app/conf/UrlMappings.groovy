class UrlMappings {

	static mappings = {

        "/submit/record"(controller: "mobile") { action = [POST: "submitRecord"] }
        "/submit/recordMultiPart"(controller: "mobile"){ action = [POST:"submitRecordMultipart"] }
        "/mobile/submitRecord"(controller: "mobile"){ action = [POST:"submitRecord"] }
        "/mobile/submitRecordMultipart"(controller: "mobile"){ action = [POST:"submitRecordMultipart"] }
        "/record/csv"(controller: "record"){ action = [GET:"csv"] }
        "/record/"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/record"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/record/"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/record/count"(controller: "record"){ action = [GET:"count"] }
        "/record/user/$userId"(controller: "record", action: "listForUser")
        "/record/sync/all"(controller: "record"){ action = [GET:"resyncAll"] }
        "/record/sync/$id"(controller: "record"){ action = [GET:"resyncRecord"] }
        "/record/images"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/record/images/"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/record/$id"(controller: "record"){ action = [GET:"getById", PUT:"updateById", DELETE:"deleteById", POST:"updateById"] }
        "/images"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/images/"(controller: "record"){ action = [GET:"listRecordWithImages"] }
        "/images/update"(controller: "record"){ action = [POST:"updateImages"] }
        "/location/fix"(controller: "location"){ action = [GET:"fixupLocations"] }
        "/location"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/location/"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/location/user/$userId"(controller: "location"){ action = [GET:"listForUser", DELETE: "deleteAllForUser"] }
        "/location/$id"(controller: "location"){ action = [GET:"getById", PUT:"updateById", DELETE:"deleteById", POST:"updateById"] }
        "/media/$dir/$fileName"(controller: "media"){ action = [GET:"getImage"] }

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

        "/ws/site/$id/poi" {
            controller = 'site'
            action = [POST:'createPoi']
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

        "/ws/$entity/$id/documents" {
            controller = 'document'
            action = 'find'
        }

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
