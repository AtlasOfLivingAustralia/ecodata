<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a Named Layer is the basic building block of an SLD document -->
    <NamedLayer>
        <Name>point_circle</Name>
        <UserStyle>
            <!-- Styles can have names, titles and abstracts -->
            <Title>Point circle</Title>
            <Abstract>A sample style that draws a point</Abstract>
            <!-- FeatureTypeStyles describe how to render different features -->
            <!-- A FeatureTypeStyle for rendering points -->
            <FeatureTypeStyle>
                <Name>Point</Name>
                <Rule>
                    <Name>point</Name>
                    <Title>All activities</Title>
                    <Abstract>A circle with a red fill and no stroke</Abstract>
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
                                    <ogc:Literal>5</ogc:Literal>
                                </ogc:Function>
                            </Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
