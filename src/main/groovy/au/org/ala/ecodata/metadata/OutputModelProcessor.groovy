package au.org.ala.ecodata.metadata


import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Define the set of valid data types for an output & provides a data type based switch to method to help
 * me make sure all valid types are handled when processing output metadata.
 */
class OutputModelProcessor {

    static Log log = LogFactory.getLog(OutputModelProcessor.class)

    interface ProcessingContext {}

    interface Processor<T extends ProcessingContext> {
        def number(node, T context)

        def integer(node, T context)

        def text(node, T context)

        def time(node, T context)

        def date(node, T context)

        def image(node, T context)

        def embeddedImages(node, T context)

        def species(node, T context)

        def stringList(node, T context)

        def booleanType(node, T context)

        def document(node, T context)

        def feature(node, T context)
    }

    def processNode(processor, node, context) {

        def type = node.dataType
        if (type == null) {
            log.warn("Found node with null dataType: "+node)
            return
        }

        if (context == null){
            log.warn("Found node without value" +node)
            return
        }

        switch(type) {
            case 'number':
                processor.number(node, context);
                break;
            case 'integer':
                processor.integer(node, context);
                break;
            case 'time':
                processor.time(node, context);
                break;
            case 'text':
                processor.text(node, context);
                break;
            case 'date':
            case 'simpleDate':
                processor.date(node, context);
                break;
            case 'image':
                processor.image(node, context);
                break;
            case 'embeddedImages':
                processor.embeddedImages(node, context);
                break;
            case 'species':
                processor.species(node, context);
                break;
            case 'stringList':
                processor.stringList(node, context)
                break;
            case 'boolean':
                processor.booleanType(node, context)
                break;
            case 'lookupRange':
            case 'lookupTable':
            case 'lookupByDiscreteValues':
                break; // do nothing
            case 'document':
                processor.document(node, context)
                break
            case 'masterDetail':
                break // do nothing, not supported yet
            case 'geoMap':
                processor.text(node, context)
                break
            case 'set':
                break
            case 'feature':
                processor.feature(node, context)
                break
            default:
                throw new RuntimeException("Unexpected data type: ${node.dataType}")
        }
    }

    /**
     * Takes an output containing potentially nested values and produces a flat List of stuff.
     * If the output contains more than one set of nested properties, the number of items returned will
     * be the sum of the nested properties - any particular row will only contain values from one of the
     * nested rows.
     * @param output the data to flatten
     * @param outputMetadata description of the output to flatten
     * @param duplicationNonNestedValues true if each item in the returned list contains all of the non-nested data in the output
     */
    List flatten(Map output, OutputMetadata outputMetadata, boolean duplicateNonNestedValues = true) {

        List rows = []

        def flat = output.data?:[:]
        if (duplicateNonNestedValues) {
            flat += output
        }
        def nested = outputMetadata.getNestedPropertyNames()
        if (!nested || !duplicateNonNestedValues) {
            rows << flat
        }
        if (nested) {
            Map toRepeat = duplicateNonNestedValues?flat:getRepeatingData(flat, outputMetadata)
            nested.each { property ->
                Collection nestedData = flat.remove(property)
                nestedData.each { row ->
                    rows << (row + toRepeat)
                }
            }
        }
        rows
    }

    /**
     * Takes an output containing potentially nested values and produces a flat List of stuff.
     * If the output contains more than one set of nested properties, the number of items returned will
     * be the sum of the nested properties - any particular row will only contain values from one of the
     * nested rows.
     * @param output the data to flatten
     * @param outputMetadata description of the output to flatten
     * @param duplicationNonNestedValues true if each item in the returned list contains all of the non-nested data in the output
     */
    List flatten2(Map output, OutputMetadata outputMetadata) {
        Map data = output.remove('data') ?: [:]
        data += output

        flattenNode(data, '', outputMetadata.getNestedPropertyNames())
    }

    private List flattenList(String path, List values, List nestedPropertyNames) {
        List results = []

        values.each { Map node ->
           results.addAll(flattenNode(node, path, nestedPropertyNames))
        }
        results
    }

    private String fullPath(String path, String propertyName) {
        path ? path+'.'+propertyName : propertyName
    }
    private Map nestedPropertiesByName(Map node, String path, List nestedPropertyNames) {
       node.findAll{key, value -> fullPath(path, key) in nestedPropertyNames}
    }

    private List flattenNode(Map node, String path, List nestedPropertyNames) {

        List results = []
        nestedPropertiesByName(node, path, nestedPropertyNames).each { String property, List nestedList ->
            node.remove(property)
            List nestedResults = flattenList(property, nestedList, nestedPropertyNames)

            // Prepend the current path to the keys in the returned list if necessary.
            if (path) {
                nestedResults = nestedResults.collect {Map result ->
                    result.collectEntries{k, v -> [(path+'.'+k), v]}
                }
            }
            results.addAll(nestedResults)
        }
        // If there are nested properties, combine the results of flattening the list with the
        // non-nested values of this node, otherwise just return a single result containing the node
        Map nonNestedData = node
        if (path) {
            nonNestedData = node.collectEntries {k, v -> [(fullPath(path, k)):v]}
        }
        results = results.collect{ it+nonNestedData }
        if (!results) {
            results << nonNestedData
        }
        results
    }

    def hideMemberOnlyAttributes(Map output, OutputMetadata outputMetadata, boolean userIsProjectMember = false) {

        if (userIsProjectMember) {
            return
        }

        Map result = output.data?:[:]
        def memberOnly = outputMetadata.getMemberOnlyPropertyNames()
        if (memberOnly && !userIsProjectMember) {
            memberOnly.each { property ->
                result[property] = "----"
            }
        }
    }

    Map getRepeatingData(Map data, OutputMetadata outputMetadata) {

        Map result = [:]
        ['text', 'stringList'].each {
            outputMetadata.getNamesForDataType(it, null).each {name, val ->
                if (val == true) {
                    result[name] = data[name]
                }
            }
        }
        result
    }

}
