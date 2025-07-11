package au.org.ala.ecodata

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
        "/ws/record/listHarvestDataResource" (controller: "harvest", action: "listHarvestDataResource")
        "/ws/record/listRecordsForDataResourceId" (controller: "harvest", action: "listRecordsForDataResourceId") //dataResourceId

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

        "/ws/shapefile" (controller: "spatial"){ action = [POST:"uploadShapeFile"] }
        "/ws/shapefile/geojson/$shapeFileId/$featureId"(controller: "spatial"){ action = [GET:"getShapeFileFeatureGeoJson"] }

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

        "/ws/managementunit/$action" {
            controller = 'managementUnit'
        }

        "/ws/bulkImport" { controller = 'bulkImport'
            action = [GET: 'list', POST:'create']
        }
        "/ws/bulkImport/$id" { controller = 'bulkImport'
            action = [GET: 'get', PUT:'update']
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

        "/reporting/ws/$controller/$action?/$id?(.$format)?" {
        }

        "/ws/$controller/$action?/$id?(.$format)?" {
		}

        "/reporting/$controller/$action?/$id?(.$format)?" {
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

        "/ws/$entity/$id/projects" {
            controller = 'project'
            action = 'findByAssociation'
        }

        "/ws/programs" {
            controller = 'program'
            action = 'getPrograms'
        }
        "/ws/program/findByName"(controller:"program"){ action = [GET:"findByName"] }
        "/ws/program/listOfAllPrograms"(controller: "program"){action = [GET: "listOfAllPrograms"]}
        "/ws/permissions/deleteUserPermission/$id"(controller: "permissions"){action = [POST: "deleteUserPermission"]}

        "/ws/managementUnits" {
            controller = 'managementUnit'
            action = 'getManagementUnits'
        }

        "/ws/managementUnit/$id"(controller: "managementUnit"){ action = [GET:"get", POST: "post"] }
        "/ws/managementUnit/managementUnitSiteMap"(controller: "managementUnit", action:"managementUnitSiteMap")
        "/ws/managementUnit/findByName"(controller:"managementUnit"){ action = [GET:"findByName"] }


        "/ws/report/runReport"(controller:"report", action:"runReport")
        "/ws/report/generateReportsInPeriod"(controller:"report", action:"generateReportsInPeriod")

        "/ws/project/findByName"(controller: "project"){ action = [GET:"findByName"] }
        "/ws/project/importProjectsFromSciStarter"(controller: "project", action: "importProjectsFromSciStarter")
        "/ws/project/getScienceTypes"(controller: "project"){ action = [GET:"getScienceTypes"] }
        "/ws/project/getEcoScienceTypes"(controller: "project"){ action = [GET:"getEcoScienceTypes"] }
        "/ws/project/getCountries"(controller: "project"){ action = [GET:"getCountries"] }
        "/ws/project/getUNRegions"(controller: "project"){ action = [GET:"getUNRegions"] }
        "/ws/project/getDataCollectionWhiteList"(controller: "project"){ action = [GET:"getDataCollectionWhiteList"] }
        "/ws/project/getBiocollectFacets"(controller: "project"){ action = [GET:"getBiocollectFacets"] }
        "/ws/project/getDefaultFacets"(controller: "project", action: "getDefaultFacets")
        "/ws/project/$projectId/archive"(controller: "harvest", action: "getDarwinCoreArchiveForProject")
        "/ws/project/$projectId/dataSet/$dataSetId/records"(controller: "project", action: "fetchDataSetRecords")
        "/ws/project/findStateAndElectorateForProject"(controller: "project", action: "findStateAndElectorateForProject")
        "/ws/admin/initiateSpeciesRematch"(controller: "admin", action: "initiateSpeciesRematch")
        "/ws/dataSetSummary/$projectId/$dataSetId?"(controller :'dataSetSummary') {

            action = [POST:'update', PUT:'update', DELETE:'delete']
        }

        "/ws/dataSetSummary/bulkUpdate/$projectId"(controller:'dataSetSummary', action:'bulkUpdate')
        "/ws/dataSetSummary/resync/$projectId/$dataSetId"(controller:'dataSetSummary', action:'resync')

        "/ws/document/download"(controller:"document", action:"download")

        "/ws/$controller/list"() { action = [GET:'list'] }
        "/ws/geoServer/wms"(controller: "geoServer", action: "wms")

        "/ws/document/download/$path/$filename" {
            controller = 'document'
            action = 'download'
        }

        "/ws/document/download/$filename" {
            controller = 'document'
            action = 'download'
        }

        "/ws/graphql" {
            controller = 'graphqlWs'
        }

        "/ws/paratoo/user-projects" {
            controller = 'paratoo'
            action = 'userProjects'
        }

        "/ws/paratoo/pdp/$projectId/$protocolId/read" {
            controller = 'paratoo'
            action = 'hasReadAccess'
        }

        "/ws/paratoo/pdp/$projectId/$protocolId/write" {
            controller = 'paratoo'
            action = 'hasWriteAccess'
        }

        "/ws/paratoo/get-all-collections" {
            controller = 'paratoo'
            action = [GET:'userCollections', OPTIONS:'options']
        }


        "/ws/paratoo/validate-token" {
            controller = 'paratoo'
            action = [POST:'validateToken', OPTIONS:'options']
        }

        "/ws/paratoo/mint-identifier" {
            controller = 'paratoo'
            action = [POST:'mintCollectionId', OPTIONS:'options']
        }

        "/ws/paratoo/collection" {
            controller = 'paratoo'
            action = [POST:'submitCollection', OPTIONS:'options']
        }

        "/ws/paratoo/status/$id" {
            controller = 'paratoo'
            action = 'collectionIdStatus'
        }

        "/ws/paratoo/plot-selections" {
            controller = 'paratoo'
            action = [POST: 'addPlotSelection', OPTIONS:'options', PUT: 'updatePlotSelection', GET:'getPlotSelections']
        }

        "/ws/paratoo/user-role" {
            controller = 'paratoo'
            action = [GET: 'userRoles', OPTIONS: 'options']
        }

        "/ws/paratoo/projects/$id" {
            controller = 'paratoo'
            action = [POST: 'updateProjectSites', PUT: 'updateProjectSites', OPTIONS:'options']
        }

        "/ws/metadata/term/$termId?" {
            controller = 'metadata'
            action = [POST: 'updateTerm', DELETE: 'deleteTerm']
        }

        "/"(redirect:[controller:"documentation"])
		"500"(view:'/error')
	}
}
