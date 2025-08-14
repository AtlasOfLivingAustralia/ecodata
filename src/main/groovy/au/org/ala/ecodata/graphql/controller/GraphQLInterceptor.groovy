package au.org.ala.ecodata.graphql.controller

import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.Hub
import au.org.ala.ecodata.HubService
import au.org.ala.ecodata.PermissionService
import au.org.ala.ecodata.UserService
import groovy.transform.CompileStatic
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono


class GraphQLInterceptor implements WebGraphQlInterceptor {

    UserService userService
    HubService hubService
    PermissionService permissionService

    GraphQLInterceptor(UserService userService, HubService hubService, PermissionService permissionService) {
        this.userService = userService
        this.hubService = hubService
        this.permissionService = permissionService
    }

    @Override
    Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String userId = UserService.currentUser()?.userId
        String hubIdParam = request.uri.queryParams.getFirst("id")

        if (!userId || !hubIdParam) {
            return Mono.error(new IllegalArgumentException("Hub or user not found"))
        }

        Map hubMap = hubService.findByUrlPath(hubIdParam)

        if (!hubMap) {
            hubMap = hubService.get(hubIdParam)?.properties
        }

        if (!hubMap) {
            return Mono.error(new IllegalArgumentException("Hub not found for id: ${hubIdParam}"))
        }

        boolean canAccessAPI = permissionService.checkUserPermission(userId, (String)hubMap.hubId, Hub.class.name, AccessLevel.readOnly.code)
        if (!canAccessAPI) {
            return Mono.error(new IllegalArgumentException("User ${userId} does not have permission to access the API for hub: ${hubIdParam}"))
        }
        request.configureExecutionInput((executionInput, builder) ->
                builder.graphQLContext(Collections.singletonMap("hub", hubMap)).build())

        return chain.next(request)
    }

    // You can add more methods to handle specific GraphQL operations if needed
}
