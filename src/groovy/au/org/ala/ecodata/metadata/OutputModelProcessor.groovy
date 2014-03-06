package au.org.ala.ecodata.metadata

/**
 * Define the set of valid data types for an output & provides a data type based switch to method to help
 * me make sure all valid types are handled when processing output metadata.
 */
class OutputModelProcessor {

    interface ProcessingContext {}

    interface Processor<T extends ProcessingContext> {
        def number(node, T context)

        def text(node, T context)

        def date(node, T context)

        def image(node, T context)

        def species(node, T context)
    }

    def processNode(processor, node, context) {

        def type = node.dataType
        switch(type) {
            case 'number':
                processor.number(node, context);
                break;
            case 'text':
                processor.text(node, context);
                break;
            case 'date':
                processor.date(node, context);
                break;
            case 'image':
                processor.image(node, context);
                break;
            case 'species':
                processor.species(node, context);
                break;
            default:
                throw new RuntimeException("Unexpected data type: ${node.dataType}")
        }
    }








}
