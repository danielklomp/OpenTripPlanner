/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analyst.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.core.TileExamples;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Tile {
    final GridGeometry2D gg;
    final int width, height;
    final byte UNREACHABLE = Byte.MIN_VALUE;
    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);
    public static final Map<Style, IndexColorModel> modelsByStyle;

    public abstract Sample[] getSamples();

    Tile(TileRequest req) {
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, req.width, req.height);
        this.gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope)(req.bbox));
        // TODO: check that gg intersects graph area
        LOG.debug("preparing tile for {}", gg.getEnvelope2D());
        this.width = gridEnv.width;
        this.height = gridEnv.height;
    }

    private static IndexColorModel interpolatedColorMap(int[][] breaks) {
        byte[][] values = new byte[4][256];
        int[] br0 = null;
        for (int[] br1 : breaks) {
            handleIndexes(values, br0, br1);
            br0 = br1;
        }
        return new IndexColorModel(8, 256, values[0], values[1], values[2], values[3]);
    }

    private static void handleIndexes(byte[][] values, int[] br0, int[] br1) {
        if (br0 != null) {
            int i0 = br0[0];
            int i1 = br1[0];
            int steps = i1 - i0;
            for (int channel = 0; channel < 4; ++channel) {
                int v0 = br0[channel+1];
                int v1 = br1[channel+1];
                float delta = (v1 - v0) / (float) steps;
                for (int i = 0; i < steps; i++) {
                    int v = v0 + (int)(delta * i);
                    // handle negative indexes
                    int byte_i = 0x000000FF & (i0 + i);
                    values[channel][byte_i] = (byte)v;
                }
            }
        }
    }

    static {
        modelsByStyle = new EnumMap<Style, IndexColorModel>(Style.class);
        modelsByStyle.put(Style.COLOR30, ICM_STEP_COLOR_15);
        modelsByStyle.put(Style.DIFFERENCE, ICM_DIFFERENCE_15);
        modelsByStyle.put(Style.TRANSPARENT, ICM_GRAY_60); 
        modelsByStyle.put(Style.MASK, ICM_MASK_60);
        modelsByStyle.put(Style.BOARDINGS, buildBoardingColorMap());
    }
    
    private static IndexColorModel buildOldDefaultColorMap() {
    	Color[] palette = new Color[256];
    	final int ALPHA = 0x60FFFFFF; // ARGB
    	for (int i = 0; i < 28; i++) {

            fillPalette(palette, ALPHA, i);
        }
    	for (int i = 28; i < 30; i++) {
            fillPalette(palette, ALPHA, i);
    	}
        for (int i = 150; i < palette.length; i++) {
        	palette[i] = new Color(0x00000000, true);
        }
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        for (int i = 0; i < palette.length; i++) {
        	r[i] = (byte)palette[i].getRed();
        	g[i] = (byte)palette[i].getGreen();
        	b[i] = (byte)palette[i].getBlue();
        	a[i] = (byte)palette[i].getAlpha();
        }
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    private static void fillPalette(Color[] palette, int ALPHA, int i) {
        palette[i + 00] =  new Color(ALPHA & Color.HSBtoRGB(0.333f, i * 0.037f, 0.8f), true); // Green
        palette[i + 30] =  new Color(ALPHA & Color.HSBtoRGB(0.666f, i * 0.037f, 0.8f), true); // Blue
        palette[i + 60] =  new Color(ALPHA & Color.HSBtoRGB(0.144f, i * 0.037f, 0.8f), true); // Yellow
        palette[i + 90] =  new Color(ALPHA & Color.HSBtoRGB(0.000f, i * 0.037f, 0.8f), true); // Red
        palette[i + 120] = new Color(ALPHA & Color.HSBtoRGB(0.000f, 0.000f, (29 - i) * 0.0172f), true); // Black
    }

    private static IndexColorModel buildBoardingColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        Arrays.fill(a, (byte) 80);
        g[0] = (byte) 255;
        b[1] = (byte) 255;
        r[2] = (byte) 255;
        g[2] = (byte) 255;
        r[3] = (byte) 255;
        a[255] = 0;
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    protected BufferedImage getEmptyImage(Style style) {
        IndexColorModel colorModel = modelsByStyle.get(style);
        if (colorModel == null)
            return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        else
            return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    }
    


    public BufferedImage generateImage(TimeSurface surf, RenderRequest renderRequest) {
        long currentTime = System.currentTimeMillis();
        BufferedImage image = getEmptyImage(renderRequest.style);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        for (Sample sample : getSamples()) {
            byte pixel = getRenderPixel(surf, renderRequest, sample);
            imagePixelData[i] = pixel;
            i++;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - currentTime);
        return image;
    }

    private byte getRenderPixel(TimeSurface surf, RenderRequest renderRequest, Sample sample) {
        byte pixel;
        if (sample != null) {
            if (renderRequest.style == Style.BOARDINGS) {
                pixel = 0;
            } else {
                pixel = getPixel(surf, sample);
            }
        } else {
            pixel = UNREACHABLE;
        }
        return pixel;
    }

    private byte getPixel(TimeSurface surf, Sample sample) {
        byte pixel;
        long renderRequest = sample.eval(surf);
        if (renderRequest == Long.MAX_VALUE)
            pixel = UNREACHABLE;
        else {
            renderRequest /= 60;
            renderRequest = setBounderiesEndSample(renderRequest);
            pixel = (byte) renderRequest;
        }
        return pixel;
    }

    public BufferedImage linearCombination(
            double k1, TimeSurface surfA,
            double k2, TimeSurface surfB,
            double intercept, RenderRequest renderRequest) {
        long currentTime = System.currentTimeMillis();
        BufferedImage image = getEmptyImage(renderRequest.style);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        for (Sample sample : getSamples()) {
            byte pixel = UNREACHABLE;
            if (sample != null) {
                long sampleSurfA = sample.eval(surfA);
                long sampleSurfB = sample.eval(surfB);
                if (sampleSurfA != Long.MAX_VALUE && sampleSurfB != Long.MAX_VALUE) {
                    double endSample = (k1 * sampleSurfA + k2 * sampleSurfB) / 60 + intercept;
                    endSample = setBounderiesEndSample(endSample);
                    pixel = (byte) endSample;
                }
            }
            imagePixelData[i] = pixel;
            i++;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - currentTime);
        return image;
    }

    private double setBounderiesEndSample(double endSample) {
        if (endSample < -120)
            endSample = -120;
        else if (endSample > 120)
            endSample = 120;
        return endSample;
    }

    public GridCoverage2D getGridCoverage2D(BufferedImage image) {
        GridCoverage2D gridCoverage = new GridCoverageFactory()
            .create("isochrone", image, gg.getEnvelope2D());
        return gridCoverage;
    }



    public static BufferedImage getLegend(Style style, int width, int height) {
        IndexColorModel model = modelsByStyle.get(style);
        int startVal = 0;
        int finalVal = 150;
        int labelSpacing = 30;
        width = setControlledWidth(width);
        height = setControlledHeight(height);

        if (model == null)
            return null;

        if (style == Style.DIFFERENCE) {
            startVal = -120;
            finalVal = 120;
            labelSpacing = 30;
        }
	    int bandsTotal = finalVal - startVal;

        WritableRaster raster = model.createCompatibleWritableRaster(width, height);
        byte[] pixels = ((DataBufferByte) raster.getDataBuffer()).getData();
        for (int row = 0; row < height; row++)
            for (int col = 0; col < width; col++)
                pixels[row * width + col] = (byte) (startVal + col * bandsTotal / width);

        BufferedImage legend = model.convertToIntDiscrete(raster, false);
        Graphics2D graphics2D = setGraphics2DProperties(legend);
        float scale = width / (float) bandsTotal;

        for (int i = startVal; i < bandsTotal; i += labelSpacing)
            graphics2D.drawString(Integer.toString(i), scale * (-startVal + i), height);
        return legend;
    }

    private static Graphics2D setGraphics2DProperties(BufferedImage legend) {
        Graphics2D gr = legend.createGraphics();
        gr.setColor(new Color(0));
        gr.drawString("travel time (minutes)", 0, 10);
        return gr;
    }

    private static int setControlledHeight(int height) {
        if (height < 25 || height > 2000)
            height = 25;
        return height;
    }

    private static int setControlledWidth(int width) {
        if (width < 140 || width > 2000)
            width = 140;
        return width;
    }


}
