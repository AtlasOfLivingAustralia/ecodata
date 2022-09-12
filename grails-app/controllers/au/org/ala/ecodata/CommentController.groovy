package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.DELETED

import grails.converters.JSON
import groovy.json.JsonSlurper

import static org.apache.http.HttpStatus.*;

class CommentController {
    CommentService commentService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        //response.setContentType("application/json;charset=UTF-8")
        render model as JSON
    }

    def list() {
        List comments = []
        String sort = params.sort ?: "lastUpdated"
        String orderBy = params.order ?: "desc"
        Integer startFrom = (params.start ?: "0") as Integer
        Integer max = (params.pageSize ?: "10") as Integer
        Boolean sortOrder = orderBy == 'asc';
        String entityId = params.entityId
        String entityType = params.entityType
        Integer total;
        if (!entityId || !entityType) {
            response.sendError(SC_BAD_REQUEST, 'Insufficient parameters provided. Missing either entityId or entityType')
        } else {
            total = Comment.countByEntityIdAndEntityTypeAndParentIsNullAndStatusNotEqual(entityId, entityType, DELETED);

            Comment.withCriteria {
                eq "entityId", entityId
                eq "entityType", entityType
                isNull "parent"
                ne "status", DELETED

                maxResults max
                order sort, orderBy
                offset startFrom
            }.each {
                Map comment = commentService.getCommentProperties(it)
                if (comment.children?.size()) {
                    commentService.sortCommentChildren(comment.children, sortOrder);
                }
                comments.add(comment)
            }

            render text: [total: total, items: comments] as JSON, contentType: 'application/json'
        }
    }


    @RequireApiKey
    def create() {
        def jsonSlurper = new JsonSlurper()
        Object json = jsonSlurper.parse(request.getReader())
        if (!json.userId) {
            response.sendError(SC_BAD_REQUEST, 'Missing userId')
        } else if (!json.entityId || !json.entityType) {
            response.sendError(SC_BAD_REQUEST, 'Missing entityId and/or entityType')
        } else if (!json.text) {
            response.sendError(SC_BAD_REQUEST, 'Missing text');
        } else {
            Comment comment = commentService.create(json);
            if(!comment.hasErrors()){
                Map model = commentService.getCommentProperties(comment)

                response.addHeader("content-location", grailsApplication.config.getProperty('grails.serverURL') + "/comment/" + comment.getId().toString())
                response.addHeader("location", grailsApplication.config.getProperty('grails.serverURL') + "/comment/" + comment.getId().toString())
                response.addHeader("entityId", comment.getId().toString())
                response.setContentType("application/json")
                render new JSON(model)
            } else {
                response.sendError(SC_INTERNAL_SERVER_ERROR, 'Failed saving data to database');
            }
        }

    }

    @RequireApiKey
    def update() {
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        if (!json.id) {
            response.sendError(SC_BAD_REQUEST, "Missing id");
        } else if (!json.text) {
            response.sendError(SC_BAD_REQUEST, 'Missing text');
        } else {
            Map result
            json.isALAAdmin = (json?.isALAAdmin != null ? json?.isALAAdmin?.toString()?.toBoolean() : false)
            Comment comment = commentService.update(json);
            if(comment){
                if ((comment.userId == json.userId) || json.isALAAdmin) {
                    result = commentService.getCommentProperties(comment);
                    if (comment.hasErrors()) {
                        result.success = false;
                        response.status = SC_INTERNAL_SERVER_ERROR;
                        result.message = comment.getErrors();
                    } else {
                        result.success = true;
                    }

                    render(text: new JSON(result), contentType: 'application/json');
                } else {
                    response.sendError(SC_UNAUTHORIZED, 'Only comment owner can update this comment.');
                }
            } else {
                response.sendError(SC_NOT_FOUND, 'Comment not found');
            }
        }
    }

    @RequireApiKey
    def delete() {
        if (!params.id) {
            response.sendError(SC_BAD_REQUEST, "Missing id");
        } else {
            params.isALAAdmin = params.boolean('isALAAdmin');

            boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()

            Comment comment = Comment.get(params.id)
            if (comment) {
                if ((comment.userId == params.userId) || params.isALAAdmin || commentService.canUserEditOrDeleteComment(params.userId, params.entityId, params.entityType) ) {
                    Map msg = commentService.delete(params.id, destroy)

                    render(text: msg as JSON, contentType: 'application/json');
                } else {
                    response.sendError(SC_UNAUTHORIZED, 'Only comment owner can delete this comment.');
                }
            } else {
                response.sendError(SC_NOT_FOUND, 'Comment not found');
            }
        }
    }

    def get() {
        if (!params.id) {
            response.sendError(SC_BAD_REQUEST, "Missing id");
        } else {
            Comment c = Comment.get(params.id);
            if (c) {
                Map mapOfProperties = commentService.getCommentProperties(c)
                render text: new JSON(mapOfProperties), contentType: 'application/json'
            } else {
                response.sendError(SC_NOT_FOUND, "Comment not found");
            }
        }
    }

    @RequireApiKey
    def canUserEditOrDeleteComment(){
        if(!params.userId || !params.entityId || !params.entityType){
            response.sendError(SC_BAD_REQUEST, "Missing userId, entityId or entityType")
        } else {
            Map result = [:];
            result.isAdmin = commentService.canUserEditOrDeleteComment(params.userId, params.entityId, params.entityType)
            asJson(result);
        }
    }
}
