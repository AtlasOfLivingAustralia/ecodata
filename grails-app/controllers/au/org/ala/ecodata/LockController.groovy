package au.org.ala.ecodata

class LockController {

    LockService lockService
    UserService userService

    def get(String id) {
        render Lock.get(id)
    }

    def lock(String id) {
        render lockService.lock(id)
    }

    def unlock(String id) {
        render lockService.unlock(id)
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
