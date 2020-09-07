<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a Named Layer is the basic building block of an SLD document -->
    <NamedLayer>
        <Name>line_selector</Name>
        <UserStyle>
            <!-- Styles can have names, titles and abstracts -->
            <Title>Sites with line</Title>
            <Abstract>Sites with line</Abstract>
            <!-- FeatureTypeStyles describe how to render different features -->
            <!-- A FeatureTypeStyle for rendering points -->
            <FeatureTypeStyle>
                <Name>line</Name>
                <Rule>
                    <Name>lineselector</Name>
                    <Title>Activities with line sites</Title>
                    <Abstract>Style used by GetFeatureInfo to select line sites.
                        Elasticgeo plugin does not support function.
                        GetFeatureInfo by default add geometryType function if
                        LineSymobizer is added to style. Therefore, using PointSymbolizer here.
                    </Abstract>
                    <ogc:Filter>
                        <ogc:Or>
                            <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>sites.geometryType</ogc:PropertyName>
                                <ogc:Literal>LineString</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
                            <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>sites.geometryType</ogc:PropertyName>
                                <ogc:Literal>MultiLineString</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
                        </ogc:Or>
                    </ogc:Filter>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#FF0000</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>
                                <ogc:Function name="env">
                                    <ogc:Literal>size</ogc:Literal>
                                    <ogc:Literal>30</ogc:Literal>
                                </ogc:Function>
                            </Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>