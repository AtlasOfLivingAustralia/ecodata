package au.org.ala.ecodata.reporting

import grails.converters.JSON
import groovy.util.logging.Log4j
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Created by sat01a on 26/09/2014.
 */
@Log4j
class SummaryXlsExporter {

    def scoreHeaders = ['Category','Title', 'GMS ID','Description','List name','Score Name','Score OutputName', 'Score Units']
    def scoreProperties = ['category','label','gmsId','description','listName','name', 'outputName','units']

    def resultHeaders = ['Category','Group Title','Count','Group','Results','Units']
    def resultProperties = ['category','groupTitle','count','group','result','units']

    def XlsExporter exporter

    public SummaryXlsExporter(XlsExporter exporter) {
        this.exporter = exporter
    }

    public exportAll(results){

        AdditionalSheet sheet = exporter.addSheet('Scores', scoreHeaders)
        int row = sheet.getSheet().lastRowNum
        results?.each {type ->
            type?.value?.each{ typeDetail ->
                sheet.add([typeDetail?.score], scoreProperties, row+1)
                row = sheet.getSheet().lastRowNum
            }
        }

        AdditionalSheet resultsSheet = exporter.addSheet('Results', resultHeaders)
        int resultRow = resultsSheet.getSheet().lastRowNum
        results?.each {category ->
            category?.value?.each{ type ->
                type?.results?.each{ item ->
                    def customOutput = [category: category.key, groupTitle:type.groupTitle,count:item.count, group:item.group, result:item.result,units:item.units]
                    if(customOutput.result instanceof groovy.lang.MapWithDefault) {
                        customOutput.result = customOutput.result as String
                    }
                    resultsSheet.add([customOutput], resultProperties, resultRow + 1)
                    resultRow = resultsSheet.getSheet().lastRowNum
                }
            }
        }
    }
}
