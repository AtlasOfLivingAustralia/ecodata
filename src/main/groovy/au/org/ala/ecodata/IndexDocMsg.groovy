package au.org.ala.ecodata

/**
 * Simple bean to store message queue data
 *
 * @author Nick dos Remedios (nick.dosremedios@csiro.au)
 */
class IndexDocMsg {
    def docType
    def docId
    def indexType
    List docIds

    public String toString() {
        "IndexDocMsg[docType: ${docType}; docId: ${docId}; indexType: ${indexType}; docIds: ${docIds.join('|')}]"
    }
}
