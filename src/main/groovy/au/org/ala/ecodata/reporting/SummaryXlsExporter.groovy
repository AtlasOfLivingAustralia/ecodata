package au.org.ala.ecodata.reporting

import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Created by sat01a on 26/09/2014.
 */
class SummaryXlsExporter {

    def scoreHeaders = ['Category','Title', 'GMS ID','Description','List name','Score Name','Score OutputName', 'Score Units']
    def scoreProperties = ['category','label','gmsId','description','listName','name', 'outputName','units']

    def resultHeaders = ['Category','Title','Count','Group','Results','Units']
    def resultProperties = ['category','groupTitle','count','group','result','units']

    def XlsExporter exporter

    public SummaryXlsExporter(XlsExporter exporter) {
        this.exporter = exporter
    }

    public exportAll(results){

        AdditionalSheet sheet = exporter.addSheet('Scores', scoreHeaders)
        int row = sheet.getSheet().lastRowNum
        results?.each {category ->
            category?.value?.each{ result ->
                sheet.add([result], scoreProperties, row+1)
                row = sheet.getSheet().lastRowNum
            }
        }

        AdditionalSheet resultsSheet = exporter.addSheet('Results', resultHeaders)
        int resultRow = resultsSheet.getSheet().lastRowNum
        results?.each {category ->
            category?.value?.each{ result ->
                def customOutput = [category: category.key, groupTitle:result.label,count:result.result.count, group:result.result.group, result:result.result,units:result.units]
                if(customOutput.result instanceof groovy.lang.MapWithDefault) {
                    customOutput.result = customOutput.result as String
                }
                resultsSheet.add([customOutput], resultProperties, resultRow + 1)
                resultRow = resultsSheet.getSheet().lastRowNum

            }
        }
    }
}
