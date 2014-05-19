package au.org.ala.ecodata.metadata

import groovy.util.logging.Log4j

/**
 * Define the set of valid data types for an output & provides a data type based switch to method to help
 * me make sure all valid types are handled when processing output metadata.
 */
@Log4j
class OutputModelProcessor {

    interface ProcessingContext {}

    interface Processor<T extends ProcessingContext> {
        def number(node, T context)

        def integer(node, T context)

        def text(node, T context)

        def date(node, T context)

        def image(node, T context)

        def embeddedImages(node, T context)

        def species(node, T context)

        def stringList(node, T context)

        def booleanType(node, T context)
    }

    def processNode(processor, node, context) {

        def type = node.dataType
        if (type == null) {
            log.warn("Found node with null dataType: "+node)
            return;
        }
        switch(type) {
            case 'number':
                processor.number(node, context);
                break;
            case 'integer':
                processor.integer(node, context);
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
                break; // do nothing
            default:
                throw new RuntimeException("Unexpected data type: ${node.dataType}")
        }
    }








}
