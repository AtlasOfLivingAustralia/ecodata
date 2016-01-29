class UrlMappings {

	static mappings = {


        "/ws/record"(controller: "record"){ action = [GET:"list", POST:"create"] }
        "/ws/record/"(controller: "record"){ action = [GET:"list", POST:"create"] }

        "/ws/record/export"(controller: "record"){ action = [GET:"export"] }
        "/ws/record/csvProject"(controller: "record"){ action = [GET:"csvProject"] }
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
        "/ws/comment/canUserEditOrDeleteComment"(controller: "comment", action: "canUserEditOrDeleteComment")

        "/ws/audit/getAuditMessagesForProjectPerPage/$id"(controller: "audit", action: "getAuditMessagesForProjectPerPage")

        "/ws/document/listImages"(controller: "document", action: "listImages")

        "/ws/site/getImages"( controller: 'site', action: 'getImages')
        "/ws/site/getPoiImages"( controller: 'site', action: 'getPoiImages')

        "/ws/activitiesForProject/$id" {
            controller = 'activity'
            action = 'activitiesForProject'
        }
        "/ws/deleteByProjectActivity/$id" {
            controller = 'activity'
            action = 'deleteByProjectActivity'
        }
        "/ws/listForUser/$id?" {
            controller = 'activity'
            action = 'listForUser'
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

        "/ws/output" {
            controller = 'output'
            action = [GET: 'list']
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

        "/ws/$entity/$id/reports" {
            controller = 'report'
            action = 'find'
        }

        "/ws/user/$id/reports" {
            controller = 'report'
            action = 'findByUserId'
        }

        "/ws/project/findByName"(controller: "project"){ action = [GET:"findByName"] }

        "/"(view:"/index")
		"500"(view:'/error')
	}
}
