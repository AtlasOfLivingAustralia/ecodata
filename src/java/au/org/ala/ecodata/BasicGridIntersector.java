package au.org.ala.ecodata;

import groovy.util.logging.Log;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is used to return the appropriate grid cell value from an on-disk DIVA grid that corresponds with a supplied lat/long.
 * Is is based on the class org.ala.layers.grid.GridGroup in the layers-store project
 */
public class BasicGridIntersector {


    RandomAccessFile raf;
    byte[] b;
    public Boolean byteorderLSB;
    public int ncols, nrows;
    public double nodatavalue;
    public Boolean valid;
    public double[] values;
    public double xmin, xmax, ymin, ymax;
    public double xres, yres;
    public String datatype;


    public BasicGridIntersector(String filePath) throws Exception {
        readgrd(filePath + ".grd");
        raf = new RandomAccessFile(filePath + ".gri", "r");
    }

    float readCell(double longitude, double latitude) throws Exception {
        float cellValue;
        //seek
        long pos = getcellnumber(longitude, latitude);
        if (pos >= 0) {
            b = new byte[4];
            raf.seek(pos * 4);
            raf.read(b);
            ByteBuffer bb = ByteBuffer.wrap(b);
            if (byteorderLSB) {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            return bb.getFloat();
        } else {
            throw new IllegalArgumentException("No data at this position");
        }
    }

    //transform to file position
    public int getcellnumber(double x, double y) {
        if (x < xmin || x > xmax || y < ymin || y > ymax) //handle invalid inputs
        {
            return -1;
        }

        int col = (int) ((x - xmin) / xres);
        int row = this.nrows - 1 - (int) ((y - ymin) / yres);

        //limit each to 0 and ncols-1/nrows-1
        if (col < 0) {
            col = 0;
        }
        if (row < 0) {
            row = 0;
        }
        if (col >= ncols) {
            col = ncols - 1;
        }
        if (row >= nrows) {
            row = nrows - 1;
        }
        return (row * ncols + col);
    }

    private void readgrd(String filename) throws Exception {
        IniReader ir = new IniReader(filename);

        datatype = "FLOAT";
        ncols = ir.getIntegerValue("GeoReference", "Columns");
        nrows = ir.getIntegerValue("GeoReference", "Rows");
        xmin = ir.getDoubleValue("GeoReference", "MinX");
        ymin = ir.getDoubleValue("GeoReference", "MinY");
        xmax = ir.getDoubleValue("GeoReference", "MaxX");
        ymax = ir.getDoubleValue("GeoReference", "MaxY");
        xres = ir.getDoubleValue("GeoReference", "ResolutionX");
        yres = ir.getDoubleValue("GeoReference", "ResolutionY");
        if (ir.valueExists("Data", "NoDataValue")) {
            nodatavalue = (float) ir.getDoubleValue("Data", "NoDataValue");
        } else {
            nodatavalue = Double.NaN;
        }

        String s = ir.getStringValue("Data", "ByteOrder");

        byteorderLSB = true;
        if (s != null && s.length() > 0) {
            if (s.equals("MSB")) {
                byteorderLSB = false;
            }
        }
    }

    public void close() {
        try {
            raf.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
