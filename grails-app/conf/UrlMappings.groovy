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
        "/ws/record/getRecordForOutputSpeciesId/"(controller: "record", action: "getRecordForOutputSpeciesId")
        "/ws/record/listHarvestDataResource" (controller: "record", action: "listHarvestDataResource")
        "/ws/record/listRecordsForDataResourceId" (controller: "record", action: "listRecordsForDataResourceId") //dataResourceId

        "/ws/record/$id"(controller: "record"){ action = [GET:"get", PUT:"update", DELETE:"delete", POST:"update"] }

        "/ws/activity/getDefaultFacets"(controller: "activity", action: "getDefaultFacets")


        "/ws/location"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/ws/location/"(controller: "location"){ action = [GET:"list", POST:"create"] }
        "/ws/location/user/$userId"(controller: "location"){ action = [GET:"listForUser", DELETE: "deleteAllForUser"] }
        "/ws/location/$id"(controller: "location"){ action = [GET:"get", PUT:"update", DELETE:"delete", POST:"update"] }

        "/ws/comment"(controller: "comment"){ action = [GET: 'list', POST: 'create'] }
        "/ws/comment/$id"(controller: "comment"){ action = [GET:"get", PUT:"update", DELETE:"delete", POST:"update"] }
        "/ws/comment/canUserEditOrDeleteComment"(controller: "comment", action: "canUserEditOrDeleteComment")

        "/ws/audit/getAuditMessagesForProjectPerPage/$id"(controller: "audit", action: "getAuditMessagesForProjectPerPage")

        "/ws/document/listImages"(controller: "document", action: "listImages")
        "/ws/document/createThumbnail"(controller:"document", action:"createThumbnail")
        "/ws/document/$id/file"(controller: "document", action: "getFile")

        "/ws/site/getImages"( controller: 'site', action: 'getImages')
        "/ws/site/getPoiImages"( controller: 'site', action: 'getPoiImages')

        "/ws/output/getOutputSpeciesUUID/"(controller: "output"){ action = [GET:"getOutputSpeciesUUID"] }

        "/ws/activitiesForProject/$id" {
            controller = 'activity'
            action = 'activitiesForProject'
        }
        "/ws/deleteByProjectActivity/$id" {
            controller = 'activity'
            action = 'deleteByProjectActivity'
        }

        "/ws/activityBulkDelete" {
            controller = 'activity'
            action = 'bulkDelete'
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



        //Get reports of all managements
        "/ws/managementunit/report" {
        controller = 'managementUnit'
        action = 'getFullReport'
        }

        //Get all reports of a management unit
        "/ws/managementunit/$id/report" {
            controller = 'managementUnit'
            action = 'report'
        }

        //Get reports of all managements
        "/ws/managementunit/report" {
            controller = 'managementUnit'
            action = 'getFullReport'
        }

        //Get reports of all managements
        "/ws/managementunit/$action" {
            controller = 'managementUnit'
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
            action = [POST:'createOrUpdatePoi']
        }

        "/ws/site/$id/poi/$poiId" {
            controller = 'site'
            action = [DELETE:'deletePoi']
        }

        "/ws/site/lookupLocationMetadataForSite" {
            controller = 'site'
            action = 'lookupLocationMetadataForSite'
        }

        "/ws/$controller/search" {
            action = [POST:"search"]
        }

		"/ws/$controller/$action?/$id?(.$format)?" {
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

        "/ws/$entity/$id/projects" {
            controller = 'project'
            action = 'findByAssociation'
        }

        "/ws/programs" {
            controller = 'program'
            action = 'getPrograms'
        }
        "/ws/program/findByName"(controller:"program"){ action = [GET:"findByName"] }

        "/ws/managementUnits" {
            controller = 'managementUnit'
            action = 'getManagementUnits'
        }

        "/ws/managementUnit/$id"(controller: "managementUnit"){ action = [GET:"get", POST: "post"] }
        "/ws/managementUnit/managementUnitSiteMap"(controller: "managementUnit", action:"managementUnitSiteMap")
        "/ws/managementUnit/findByName"(controller:"managementUnit"){ action = [GET:"findByName"] }


        "/ws/report/runReport"(controller:"report", action:"runReport")

        "/ws/project/findByName"(controller: "project"){ action = [GET:"findByName"] }
        "/ws/project/importProjectsFromSciStarter"(controller: "project", action: "importProjectsFromSciStarter")
        "/ws/project/getScienceTypes"(controller: "project"){ action = [GET:"getScienceTypes"] }
        "/ws/project/getEcoScienceTypes"(controller: "project"){ action = [GET:"getEcoScienceTypes"] }
        "/ws/project/getCountries"(controller: "project"){ action = [GET:"getCountries"] }
        "/ws/project/getUNRegions"(controller: "project"){ action = [GET:"getUNRegions"] }
        "/ws/project/getDataCollectionWhiteList"(controller: "project"){ action = [GET:"getDataCollectionWhiteList"] }
        "/ws/project/getBiocollectFacets"(controller: "project"){ action = [GET:"getBiocollectFacets"] }
        "/ws/project/getDefaultFacets"(controller: "project", action: "getDefaultFacets")
        "/ws/admin/initiateSpeciesRematch"(controller: "admin", action: "initiateSpeciesRematch")

        "/ws/$controller/list"() { action = [GET:'list'] }



        "/ws/$controller/$id?(.$format)?" {
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/$controller/$id?(.$format)?" {
            action = [GET: 'get', PUT:'update', DELETE:'delete', POST:'update']
        }

        "/"(redirect:[controller:"documentation"])
		"500"(view:'/error')
	}
}
