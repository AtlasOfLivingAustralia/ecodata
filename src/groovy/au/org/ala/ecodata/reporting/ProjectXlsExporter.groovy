package au.org.ala.ecodata.reporting
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.metadata.OutputModelProcessor
import pl.touk.excel.export.getters.Getter
import pl.touk.excel.export.multisheet.AdditionalSheet
/**
 * Created by god08d on 11/04/14.
 */
class ProjectXlsExporter {

    def projectHeaders = ['Name', 'Description']

    def projectProperties = ['name', 'description']
    def siteHeaders = ['Site name']
    def siteProperties = ['name']
    def activityHeaders = ['ID', 'Planned Start date', 'Planned End date', 'Description', 'Activity Type', 'Theme', 'Status']
    def activityProperties = ['activityId', 'plannedStartDate', 'plannedEndDate', 'description', 'type', 'mainTheme', 'progress']

    def metadataService
    def XlsExporter exporter

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet activitiesSheet

    Map<String, List<AdditionalSheet>> outputSheets = [:]

    public ProjectXlsExporter(XlsExporter exporter, MetadataService metadataService) {
        this.exporter = exporter
        this.metadataService = metadataService
    }

    public void export(project) {

        projectSheet()
        sitesSheet()
        activitiesSheet()

        projectSheet.add([project], projectProperties)

        if (project.sites) {
            sitesSheet.add(project.sites, siteProperties)
        }
        if (project.activites) {

            def outputsByType = [:].withDefault { [] }

            activitiesSheet.add(project.activites, activityProperties)

            project.activites.each { activity ->
                activity.outputs?.each { output ->
                    outputsByType[output.name] << output
                }
            }


            outputsByType.each { outputName, data ->
                def config = outputProperties(outputName)
                if (config.headers) {
                    if (!outputSheets[outputName]) {
                        outputSheets[outputName] = exporter.addSheet(outputName, config.headers)
                    }
                    AdditionalSheet outputSheet = outputSheets[outputName]
                    outputSheet.add(data, config.propertyGetters)
                }
            }
        }
    }

    public void exportAll(List projects) {
        projects.each { export(it) }
    }

    AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Project', projectHeaders)
        }
        projectSheet
    }

    AdditionalSheet sitesSheet() {
        if (!sitesSheet) {
            sitesSheet = exporter.addSheet('Sites', siteHeaders)
        }
        sitesSheet
    }

    AdditionalSheet activitiesSheet() {
        if (!activitiesSheet) {
            activitiesSheet = exporter.addSheet('Activities', activityHeaders)
        }
        activitiesSheet
    }


    def outputProperties(name) {
        def model = metadataService.annotatedOutputDataModel(name)

        def headers = []
        def properties = []
        model.each {
            if (it.dataType == 'list') {
                it.columns.each { col ->
                    properties << it.name+'.'+col.name
                    headers << col.label
                }
            }
            else if (it.dataType in ['photoPoints', 'matrix']) {
                // not supported, do nothing.
            }
            else {
                properties << it.name
                headers << it.description
            }
        }
        println headers
        def propertyGetters = properties.collect{new OutputDataPropertiesBuilder(it, model)}
        [headers:headers, propertyGetters:propertyGetters]
    }

    class Value implements OutputModelProcessor.ProcessingContext {
        public Value(value) {
            this.value = value
        }
        def value
    }


    class OutputDataPropertiesBuilder extends OutputModelProcessor implements OutputModelProcessor.Processor<Value>, Getter<String> {

        private String[] nameParts
        private List outputDataModel

        public OutputDataPropertiesBuilder(String name, outputDataModel) {
            this.nameParts = name.tokenize('.'[0]);
            this.outputDataModel = outputDataModel;
        }


        // Implementation of OutputModelProcessor.Processor
        @Override
        def number(Object node, Value outputValue) {
            def val = outputValue.value
            return val?val as String:""
        }

        @Override
        def integer(Object node, Value outputValue) {
            def val = outputValue.value
            return val?val as String:""
        }

        @Override
        def text(Object node, Value outputValue) {
            def val = outputValue.value
            return val?val as String:""
        }

        @Override
        def date(Object node, Value outputValue) {
            return new Value(outputValue?:"") // dates are UTC formatted strings already
        }

        @Override
        def image(Object node, Value outputValue) {
            return ""
        }

        @Override
        def embeddedImages(Object node, Value outputValue) {
            return ""
        }

        @Override
        def species(Object node, Value outputValue) {
            def val = outputValue.value

            return val?val.name:""
        }

        @Override
        def stringList(Object node, Value outputValue) {
            def val = outputValue.value
            return val?val.join(','):""
        }

        // Implementation of Getter<String>
        @Override
        String getPropertyName() {
            return nameParts.join('.');
        }

        @Override
        String getFormattedValue(Object output) {

            def node = outputDataModel
            for (String part : nameParts) {
                def tmpNode = node.find{it.name == part}
                // List typed model elements have a cols element containing nested nodes.
                node = tmpNode.columns?:tmpNode
            }

            processNode(this, node, getValue(output))

        }

        def getValue(outputModelOrData) {
            def value = outputModelOrData.data
            for (String part : nameParts) {
                value = value[part]
            }
            new Value(value)
        }
    }

}
