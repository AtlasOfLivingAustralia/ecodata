package au.org.ala.ecodata



import org.junit.*
import grails.test.mixin.*

@TestFor(OutputController)
@Mock(Output)
class OutputControllerTests {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        //params["name"] = 'someValidName'
    }

    void testIndex() {
        controller.index()
        assert "/output/list" == response.redirectedUrl
    }

    void testList() {

        def model = controller.list()

        assert model.outputInstanceList.size() == 0
        assert model.outputInstanceTotal == 0
    }

    void testCreate() {
        def model = controller.create()

        assert model.outputInstance != null
    }

    void testSave() {
        controller.save()

        assert model.outputInstance != null
        assert view == '/output/create'

        response.reset()

        populateValidParams(params)
        controller.save()

        assert response.redirectedUrl == '/output/show/1'
        assert controller.flash.message != null
        assert Output.count() == 1
    }

    void testShow() {
        controller.show()

        assert flash.message != null
        assert response.redirectedUrl == '/output/list'

        populateValidParams(params)
        def output = new Output(params)

        assert output.save() != null

        params.id = output.id

        def model = controller.show()

        assert model.outputInstance == output
    }

    void testEdit() {
        controller.edit()

        assert flash.message != null
        assert response.redirectedUrl == '/output/list'

        populateValidParams(params)
        def output = new Output(params)

        assert output.save() != null

        params.id = output.id

        def model = controller.edit()

        assert model.outputInstance == output
    }

    void testUpdate() {
        controller.update()

        assert flash.message != null
        assert response.redirectedUrl == '/output/list'

        response.reset()

        populateValidParams(params)
        def output = new Output(params)

        assert output.save() != null

        // test invalid parameters in update
        params.id = output.id
        //TODO: add invalid values to params object

        controller.update()

        assert view == "/output/edit"
        assert model.outputInstance != null

        output.clearErrors()

        populateValidParams(params)
        controller.update()

        assert response.redirectedUrl == "/output/show/$output.id"
        assert flash.message != null

        //test outdated version number
        response.reset()
        output.clearErrors()

        populateValidParams(params)
        params.id = output.id
        params.version = -1
        controller.update()

        assert view == "/output/edit"
        assert model.outputInstance != null
        assert model.outputInstance.errors.getFieldError('version')
        assert flash.message != null
    }

    void testDelete() {
        controller.delete()
        assert flash.message != null
        assert response.redirectedUrl == '/output/list'

        response.reset()

        populateValidParams(params)
        def output = new Output(params)

        assert output.save() != null
        assert Output.count() == 1

        params.id = output.id

        controller.delete()

        assert Output.count() == 0
        assert Output.get(output.id) == null
        assert response.redirectedUrl == '/output/list'
    }
}
