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

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.request.TileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicTile extends Tile {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicTile.class);
    final SampleSource sampleSource;
    
    public DynamicTile(TileRequest req, SampleSource sampleSource) {
        super(req);
        this.sampleSource = sampleSource;
    }
    
    public Sample[] getAllSamples() {
        Sample[] sampleReturnList = new Sample[width * height];
        long currentTime = System.currentTimeMillis();
        CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem2D();
        try {
            sampleReturnList = findAllSamples(sampleReturnList, crs);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - currentTime);
        return sampleReturnList;
    }

    private Sample[] findAllSamples(Sample[] sampleReturnList, CoordinateReferenceSystem crs) {
        MathTransform mathTransformer = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
        GridCoordinates2D coordinates2D = new GridCoordinates2D();
        int forLoopCounterY = 0, forLoopCounterX = 0;
        for (int currentCoordY = 0; currentCoordY < height; currentCoordY++) {
            for (int currentCoordX = 0; currentCoordX < width; currentCoordX++) {
                coordinates2D.x = currentCoordX;
                coordinates2D.y = currentCoordY;
                Sample sample = getSample(mathTransformer, coordinates2D);
                sampleReturnList[forLoopCounterY] = sample;

                forLoopCounterY++;
                if (sample != null)
                    forLoopCounterX++;
            }
        }
        LOG.debug("finished preparing tile. number of samples: {}", forLoopCounterX);
    }

    private Sample getSample(MathTransform mathTransformer, GridCoordinates2D coordinates2D) {
        DirectPosition sourcePos = getFindAndConvertCoordinates(mathTransformer, coordinates2D);

        double longitude = sourcePos.getOrdinate(0);
        double latitude = sourcePos.getOrdinate(1);
        return sampleSource.getSample(longitude, latitude);
    }

    private DirectPosition getFindAndConvertCoordinates(MathTransform mathTransformer, GridCoordinates2D coordinates2D) {
        DirectPosition sourcePos = gg.gridToWorld(coordinates2D);
        mathTransformer.transform(sourcePos, sourcePos);
        return sourcePos;
    }

}
