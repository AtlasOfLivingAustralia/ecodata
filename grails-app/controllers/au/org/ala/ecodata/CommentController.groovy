package au.org.ala.ecodata

import grails.converters.JSON
import groovy.json.JsonSlurper

class CommentController {
    def outputService

    def ignores = ["action","controller"]

    def list() {
        List comments = []
        def sort = params.sort ? params.sort : "dateCreated"
        def order = params.order ? params.order :  "desc"
        def offset = params.start ? params.start : 0
        def max = params.pageSize ? params.pageSize : 10
        Boolean sortOrder = order == 'asc'? true : false;
        String  entityId = params.entityId
        String entityType = params.entityType
        Integer total = 0;

        if( !entityId || !entityType){
            response.sendError(400, 'Insufficient parameters provided. Missing either entityId or entityType')
        } else {
//           todo: is there a better way to do count. is the below method potentially slow?
            total = Comment.findAllWhere([ 'entityId':entityId, 'entityType':entityType, parent: null]).size();
            Comment.findAllWhere(['entityId':entityId, 'entityType':entityType, parent: null], [sort:sort,order:order,offset:offset,max:max]).each {
                Map mapOfProperties = outputService.getCommentProperties(it)
                comments.add(mapOfProperties)
            }

            comments.each { comment ->
                if(comment.children?.size()){
                    outputService.sortCommentChildren(comment.children, sortOrder);
                }
            }

            render text:  [total: total, items: comments] as JSON, contentType: 'application/json'
        }
    }


    @RequireApiKey
    def create(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        if (!json.userId){
            response.sendError(400, 'Missing userId')
        } else if (!json.entityId || !json.entityType){
            response.sendError(400, 'Missing entityId and/or entityType')
        } else if ( !json.text ){
            response.sendError(400, 'Missing text');
        } else {
            Comment l = new Comment(json)
            Comment parent;
            if(l.dateCreated == null){
                l.dateCreated = new Date();
            }

            Comment comment = l.save(true)

            if(json.parent != null){
                parent = Comment.get(json.parent);
                if(parent){
                    comment.parent = parent;
                    parent.children.add(comment);
                    parent.save(true);
                    comment.save(true);
                }
            }



            response.addHeader("content-location", grailsApplication.config.grails.serverURL + "/comment/" + comment.getId().toString())
            response.addHeader("location", grailsApplication.config.grails.serverURL + "/comment/" + comment.getId().toString())
            response.addHeader("entityId", comment.getId().toString())
            response.setContentType("application/json")
            def model = outputService.getCommentProperties(comment)
            render model as JSON
        }

    }

    @RequireApiKey
    def update(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        if(!json.id){
            response.sendError(400, "Missing id");
        } else if ( !json.text ){
            response.sendError(400, 'Missing text');
        } else {
            Map result
            Comment c = Comment.get(json.id);
            if(c){
                if(c['userId'] == json['userId']){
                    c['text'] = json['text'];
                    //update time
                    c['dateCreated'] = new Date();
                    c.save(flush: true)
                    result = outputService.getCommentProperties(c);
                    if(c.hasErrors()){
                        result['success'] = false;
                        response.status = 500;
                        result['message'] = c.getErrors();
                    } else {
                        result['success'] = true;
                    }

                    render(text: result as JSON, contentType: 'application/json');
                } else {
                    response.sendError(401, 'Only comment owner can update this comment.');
                }
            } else {
                response.sendError(404, 'Comment not found');
            }
        }
    }

    @RequireApiKey
    def delete(){
        if(!params.id){
            response.sendError(400, "Missing id");
        } else {
            Comment c = Comment.get(params.id);
            if(c){
                if(c['userId'] == params['userId']){
                    c.delete( flush: true);
                    Map msg = [:];
                    if(c.hasErrors()){
                        msg['success'] = false
                        response.status = 500;
                        msg['message'] = c.getErrors();
                    } else {
                        msg['success'] = true
                    }
                    render( text: msg as JSON, contentType: 'application/json');
                } else {
                    response.sendError(401, 'Only comment owner can delete this comment.');
                }
            } else {
                response.sendError(404, 'Comment not found');
            }

        }
    }

    def get(){
        if(!params.id){
            response.sendError(400, "Missing id");
        } else {
            Comment c = Comment.get(params.id);
            if(c){
                Map mapOfProperties = outputService.getCommentProperties(c)
                render text:  mapOfProperties as JSON, contentType: 'application/json'
            } else {
                response.sendError(404, "Comment not found");
            }
        }
    }
}
