package au.org.ala.ecodata.metadata

/**
 * Created by god08d on 28/02/14.
 */
class OutputMetadata {

    private def metadata
    public OutputMetadata(metadata) {
        this.metadata = metadata
    }

    def annotateDataModel() {


        annotateNodes(metadata.dataModel)
    }

    def annotateNodes(nodes) {
        def annotatedNodes = []
        nodes.each { node->
            def annotatedNode = [:]
            annotatedNode.putAll(node)
            if (isNestedDataModelType(node)) {

                annotatedNode.columns = annotateNodes(getNestedDataModelNodes(node))

            }
            else {


                def result = findViewByName(node.name)
                if (result) {
                    def label = result.preLabel?:(result.title?:result.postLabel)
                    annotatedNode.label = label
                }


            }
            annotatedNodes << annotatedNode
        }
        annotatedNodes
    }


    def findViewByName(name, context = null) {

        if (!context) {
            context = metadata.viewModel
        }
        return context.findResult { node ->
            if (isNestedViewModelType(node)) {
                def nested = getNestedViewNodes(node)

                return findViewByName(name, nested)
            }
            else {
                return (node.source == name)?node:null
            }
        }
    }

    def getNestedViewNodes(node) {
        return (node.type == 'table') ? node.columns: node.items
    }
    def getNestedDataModelNodes(node) {
        return node.columns
    }

    def isNestedDataModelType(node) {
        return (node.columns != null)
    }
    def isNestedViewModelType(node) {
        return (node.items != null || node.columns != null)
    }

}
