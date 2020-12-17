<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a Named Layer is the basic building block of an SLD document -->
    <NamedLayer>
        <Name>polygon_sites</Name>
        <UserStyle>
            <!-- Styles can have names, titles and abstracts -->
            <Title>Sites with polygon</Title>
            <Abstract>Sites with polygon</Abstract>
            <!-- FeatureTypeStyles describe how to render different features -->
            <!-- A FeatureTypeStyle for rendering points -->
            <FeatureTypeStyle>
                <Name>Polygon</Name>
                <Rule>
                    <Name>polygonsites</Name>
                    <Title>Activities with polygon sites</Title>
                    <Abstract>Activities with polygon sites</Abstract>
                    <ogc:Filter>
                        <ogc:Or>
                            <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>sites.geometryType</ogc:PropertyName>
                                <ogc:Literal>Polygon</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
                            <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>sites.geometryType</ogc:PropertyName>
                                <ogc:Literal>MultiPolygon</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
                        </ogc:Or>
                    </ogc:Filter>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#FF0000</CssParameter>
                            <CssParameter name="opacity">
                                <ogc:Function name="env">
                                    <ogc:Literal>opacity</ogc:Literal>
                                    <ogc:Literal>0.5</ogc:Literal>
                                </ogc:Function>
                            </CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#FF0000</CssParameter>
                            <CssParameter name="stroke-width">
                                <ogc:Function name="env">
                                    <ogc:Literal>size</ogc:Literal>
                                    <ogc:Literal>1</ogc:Literal>
                                </ogc:Function>
                            </CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>