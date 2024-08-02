package au.org.ala.ecodata
@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class LockController {

    LockService lockService
    UserService userService

    def get(String id) {
        render Lock.get(id)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def lock(String id) {
        render lockService.lock(id)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def unlock(String id) {
        Map body = request.getJSON()
        render lockService.unlock(id, body.force)
    }

    def list() {
        List locks = lockService.list(params.max, params.offset)
        render locks
    }

    def findByUserId() {
        def user = userService.currentUser?.userId
        if (!user) {
            Map result = [error:'A user must be supplied for this request']
            render status:400, text:result
        }
        else {
            render Lock.findAllByUserId()
        }

    }
}
