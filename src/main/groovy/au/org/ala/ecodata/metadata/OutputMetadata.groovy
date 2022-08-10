package au.org.ala.ecodata.metadata

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Works with the Output metadata.
 */
class OutputMetadata {

    static Log log = LogFactory.getLog(OutputMetadata.class)

    /** Used to construct property names to nested model items */
    String pathSeparator = '.'

    /** The metadata template associated with this output type */
    private Map metadata

    OutputMetadata(Map metadata) {
        this.metadata = metadata
    }

    def getDarwinCoreMapping() {
        metadata.darwinCoreMapping
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
                    annotatedNode.putAll(result)
                }
                if (!annotatedNode.label) {
                    annotatedNode.label = annotatedNode.name
                }


            }
            annotatedNodes << annotatedNode
        }

        // Sort the nodes based on the order they appear in the view model for consistency.
        if (metadata.viewModel) {
            annotatedNodes.sort { node ->
                metadata.viewModel.findIndexOf{it.source == node.name}
            }
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

    Map findDataModelItemByName(String name, List context = null) {
        if (!context) {
            context = metadata.dataModel
        }
        return context.findResult { Map node ->
            if (node.name == name) {
                return node
            }
            else if (isNestedDataModelType(node)) {
                List nested = getNestedDataModelNodes(node)
                return findDataModelItemByName(name, nested)
            }
            else {
                return null
            }
        }
    }

    def getNestedPropertyNames() {
        List propertyNames = []
        dataModelIterator { String path, Map node ->
            if (isNestedDataModelType(node)) {
                propertyNames << path
            }
        }
        propertyNames
    }


    List propertyNamesAsList() {
        List propertyNames = []
        dataModelIterator{ String path, Map node ->
            propertyNames << path
        }
        propertyNames
    }

    /** Builds a property name from the current path and node name.  e.g. list.value.nestedList */
    private String fullPathToNode(String currentPath, Object node) {
        String name = node.name
        currentPath ? currentPath + pathSeparator + name : name
    }


    void dataModelIterator(Closure callback) {
        dataModelIterator('', metadata.dataModel, callback)
    }


    /**
     * Iterates over the dataModel in this model, invoking the callback for each node.
     * Handles nested properties by building a '.' separated path.
     * @param path the starting path.
     * @param nodes the nodes to iterate over
     * @param callback a closure accepting a String (the path to the data model item relative to the original path) and the current data model item
     */
    void dataModelIterator(String path, List nodes, Closure callback) {
        nodes.each { Map node ->
            String nodePath = fullPathToNode(path, node)
            callback(nodePath, node)
            if (isNestedDataModelType(node)) {
                dataModelIterator(nodePath, getNestedDataModelNodes(node), callback)
            }
        }
    }

    String getLabel(Map viewNode, Map dataNode) {
        String label = null
        if (viewNode) {
            label = viewNode.preLabel?:(viewNode.title?:viewNode.postLabel)
        }
        label ?: (dataNode.description ?: dataNode.name)
    }

    void modelIterator(Closure callback) {
        Set visitedDataNodes = new HashSet()
        visitedDataNodes = modelIteratorInternal('', metadata.viewModel, metadata.dataModel, visitedDataNodes, callback)

        // The iteration is done via matching view nodes to data nodes, so after it's complete ensure any
        // data nodes not associated with a view node are visited as well.
        dataModelIterator { path, node ->
            if (!visitedDataNodes.contains(node)) {
                println "Warning: node ${path} is not refernced by the view model"
                callback(path, null, node)
            }
        }
    }

    /**
     * Returns true if the supplied view node is referencing a node from the dataModel.
     * Not all view model nodes need to do this (e.g. rows, columns etc)
     */
    private boolean referencesDataModel(Map viewNode) {
        viewNode.source && viewNode.type != 'literal'
    }

    private Set modelIteratorInternal(String path, List viewNodes, List dataModelNodes, Set visitedDataNodes, Closure callback) {
        viewNodes.each { Map node ->
            String nestedPath = path
            List dataModelContext = dataModelNodes
            if (referencesDataModel(node)) {
                Map dataNode = findDataModelItemByName(node.source, dataModelContext)

                if (dataNode) {
                    visitedDataNodes.add(dataNode)
                    String dataNodePath = fullPathToNode(path, dataNode)
                    callback(dataNodePath, node, dataNode)

                    if (isNestedDataModelType(dataNode)) {
                        dataModelContext = getNestedDataModelNodes(dataNode)
                        nestedPath = dataNodePath
                    }
                }
                else {
                    log.warn("View node: "+node+" references missing dataModel node")
                }
            }

            if (isNestedViewModelType(node)) {
                modelIteratorInternal(nestedPath, getNestedViewNodes(node), dataModelContext, visitedDataNodes, callback)
            }
        }

        visitedDataNodes
    }




    List<String> getMemberOnlyPropertyNames() {
        List memberOnlyNames = []
        modelIterator { String path, Map viewNode, Map dataNode ->
            if (viewNode?.memberOnlyView) {
                memberOnlyNames << path
            }
        }
        memberOnlyNames
    }


    List<String> getPropertyNamesByDwcAttribute(String dwcAttribute) {
        List matchingNames = []
        dataModelIterator {String path, Map node ->
            if (node.dwcAttribute == dwcAttribute) {
                matchingNames << path
            }
        }
        matchingNames
    }


    def getNestedViewNodes(node) {
        return (node.type in ['table', 'photoPoints', 'grid'] ) ? node.columns: node.items
    }
    def getNestedDataModelNodes(node) {
        return node.columns
    }

    def isNestedDataModelType(node) {
        return (node.columns != null && node.dataType != "geoMap")
    }
    def isNestedViewModelType(node) {
        return (node.items != null || node.columns != null)
    }

    /**
     * lists all names of type passed to function
     * @param type
     * @return
     * [ 'imageList':true,
     *   'multiSightingTable':['imageTable':true]
     * ]
     *
     */
    Map getNamesForDataType(String type, context){
        Map names = [:], childrenNames

        if(!context && metadata){
            context = metadata.dataModel;
        }

        context?.each { data ->
            if(isNestedDataModelType(data)){
                // recursive call for nested data model
                childrenNames = getNamesForDataType(type, getNestedDataModelNodes(data));
                if(childrenNames?.size()){
                    names[data.name] = childrenNames
                }
            }

            if(data.dataType == type){
                names[data.name] = true
            }
        }

        return names;
    }

    static class ValidationRules {

        def validationRules = []
        public ValidationRules(property) {
            if (property.validate) {
                parseValidationRules(property.validate)
            }

        }

        private parseValidationRules(String rules) {
            def criteria = rules.tokenize(',')
            validationRules = criteria.collect { it.trim() }
        }

        private parseValidationRules(List rules) {
            validationRules = rules.collect{
                String rule = it.rule
                if (it.param && it.param.expression) {
                    rule += "[${it.param.expression}]"
                }
                rule
            }
        }

        public boolean isMandatory() {
            return validationRules.contains('required')
        }

        def min() {
            def min = validationProperties().minimum?:new BigDecimal(0)
            return min
        }

        def max() {
            return validationProperties().maximum
        }

        def validationProperties() {
            def props = [:]
            validationRules.each {
                if (it.startsWith('min')) {
                    props << minProperty(it)
                }
                else if (it.startsWith('max')) {
                    props << maxProperty(it)
                }
            }
            return props
        }

        def valueInBrackets(rule) {
            def matcher = rule =~ /.*\[(.*)\]/
            if (matcher.matches()) {
                return matcher[0][1]
            }
            return null
        }

        def minProperty(rule) {
            def min = valueInBrackets(rule)
            if (min) {
                return [minimum: min]
            }
            throw new IllegalArgumentException("Invalid validation rule: "+rule)
        }

        def maxProperty(rule) {
            def max = valueInBrackets(rule)
            if (max) {
                return [maximum: max]
            }
            throw new IllegalArgumentException("Invalid validation rule: "+rule)
        }

    }

}
