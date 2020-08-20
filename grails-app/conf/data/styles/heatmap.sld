<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>heatmap</Name>
        <UserStyle>
            <Title>Heatmap</Title>
            <Abstract>A heatmap surface showing record density</Abstract>
            <FeatureTypeStyle>
                <Transformation>
                    <ogc:Function name="gs:Heatmap">
                        <ogc:Function name="parameter">
                            <ogc:Literal>data</ogc:Literal>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>weightAttr</ogc:Literal>
                            <ogc:Function name="env">
                                <ogc:Literal>weight</ogc:Literal>
                            </ogc:Function>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>radiusPixels</ogc:Literal>
                            <ogc:Function name="env">
                                <ogc:Literal>radius</ogc:Literal>
                                <ogc:Literal>20</ogc:Literal>
                            </ogc:Function>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>pixelsPerCell</ogc:Literal>
                            <ogc:Literal>10</ogc:Literal>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>outputBBOX</ogc:Literal>
                            <ogc:Function name="env">
                                <ogc:Literal>wms_bbox</ogc:Literal>
                            </ogc:Function>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>outputWidth</ogc:Literal>
                            <ogc:Function name="env">
                                <ogc:Literal>wms_width</ogc:Literal>
                            </ogc:Function>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>outputHeight</ogc:Literal>
                            <ogc:Function name="env">
                                <ogc:Literal>wms_height</ogc:Literal>
                            </ogc:Function>
                        </ogc:Function>
                    </ogc:Function>
                </Transformation>
                <Rule>
                    <RasterSymbolizer>
                        <!-- specify geometry attribute to pass validation -->
                        <Geometry>
                            <ogc:PropertyName>geoIndex</ogc:PropertyName>
                        </Geometry>
                        <Opacity>0.6</Opacity>
                        <ColorMap type="ramp">
                            <ColorMapEntry color="#FFFFFF" quantity="0" label="nodata"
                                           opacity="0"/>
                            <ColorMapEntry color="#4444FF" quantity="0.1" label="0.1"/>
                            <ColorMapEntry color="#FF0000" quantity="0.5" label="0.5"/>
                            <ColorMapEntry color="#FFFF00" quantity="1.0" label="1.0"/>
                        </ColorMap>
                    </RasterSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
