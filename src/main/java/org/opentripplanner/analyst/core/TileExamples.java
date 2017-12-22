
package org.opentripplanner.analyst.core;

import java.awt.Color;
import java.awt.image.IndexColorModel;

public class TimeExamples {


    private static final IndexColorModel ICM_SMOOTH_COLOR_15 = interpolatedColorMap(new int[][]{
            {0, 0, 0, 0, 0},
            {15, 100, 100, 100, 80},
            {30, 0, 200, 0, 80},
            {45, 0, 0, 200, 80},
            {60, 200, 200, 0, 80},
            {75, 200, 0, 0, 80},
            {90, 200, 0, 200, 50},
            {120, 200, 0, 200, 0}
    });

    private static final IndexColorModel ICM_STEP_COLOR_15 = interpolatedColorMap(new int[][]{
            {-128, 100, 100, 100, 200},
            {0, 100, 100, 100, 0},
            {15, 100, 100, 100, 90},
            {15, 0, 140, 0, 10},
            {30, 0, 140, 0, 90},
            {30, 0, 0, 140, 10},
            {45, 0, 0, 140, 90},
            {45, 140, 140, 0, 10},
            {60, 140, 140, 0, 90},
            {60, 140, 0, 0, 10},
            {75, 140, 0, 0, 90},
            {75, 140, 0, 140, 10},
            {90, 140, 0, 140, 90},
            {90, 100, 100, 100, 50},
            {121, 100, 100, 100, 200}
    });

    private static final IndexColorModel ICM_DIFFERENCE_15 = interpolatedColorMap(new int[][]{
            {-128, 0, 0, 0, 0},
            {-127, 150, 0, 0, 80},
            {-60, 150, 0, 0, 80},
            {-15, 150, 150, 0, 80},
            {0, 150, 150, 0, 0},
            {0, 0, 0, 0, 0},
            {15, 0, 0, 150, 80},
            {45, 0, 150, 0, 90},
            {60, 100, 150, 100, 99},
            {127, 50, 150, 50, 99}
    });


    private static final IndexColorModel ICM_SAMENESS_5 = interpolatedColorMap(new int[][]{
            {-20, 80, 80, 80, 0},
            {-15, 100, 0, 100, 80},
            {-10, 0, 0, 150, 80},
            {-5, 0, 150, 0, 80},
            {0, 0, 150, 0, 150},
            {5, 0, 150, 0, 80},
            {10, 0, 0, 150, 80},
            {15, 100, 0, 100, 80},
            {20, 80, 80, 80, 0},
            {-20, 0, 0, 0, 0}
    });

    private static final IndexColorModel ICM_GRAY_60 = interpolatedColorMap(new int[][]{
            {-128, 0, 0, 0, 255},
            {0, 0, 0, 0, 255},
            {60, 0, 0, 0, 0},
            {120, 0, 0, 0, 0}
    });

    private static final IndexColorModel ICM_MASK_60 = interpolatedColorMap(new int[][]{
            {0, 0, 0, 0, 255},
            {60, 0, 0, 0, 0}
    });
}