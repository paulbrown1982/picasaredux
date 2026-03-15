package com.picasaredux;

import java.awt.*;
import java.awt.image.BufferedImage;

interface PaintableImage {
    boolean isPaintFullWidthExclusionEnabled();
    boolean isImageWiderThanTall();
    Dimension computeTargetSize(int apertureWidth, int apertureHeight);
    BufferedImage getImage();
}
