package com.picasaredux.view;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class ImageCanvas extends JPanel implements Scrollable {
    private final ImageProvider imageProvider;

    private boolean paintFullWidthExclusion;

    ImageCanvas(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    private Dimension computeTargetSize(int apertureWidth, int apertureHeight) {
        BufferedImage image = imageProvider.getImage();
        if (image == null) {
            return new Dimension(apertureWidth, apertureHeight);
        }

        float imageWidth = (float) image.getWidth();
        float imageHeight = (float) image.getHeight();

        int targetWidth = apertureWidth;
        int targetHeight = apertureHeight;

        if (isImageWiderThanTall() || paintFullWidthExclusion) {
            float aspectRatio = imageHeight / imageWidth;
            targetHeight = Math.round(apertureWidth * aspectRatio);
        } else {
            float aspectRatio = imageWidth / imageHeight;
            targetWidth = Math.round(apertureHeight * aspectRatio);
        }

        return new Dimension(targetWidth, targetHeight);
    }

    private boolean isImageWiderThanTall() {
        BufferedImage image = imageProvider.getImage();
        return image != null && image.getWidth() >= image.getHeight();
    }

    void toggleRenderingMode(boolean paintFullWidth) {
        paintFullWidthExclusion = paintFullWidth;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage image = imageProvider.getImage();
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            System.err.println("No Image");
            return;
        }

        Rectangle aperture = g.getClipBounds();
        if (aperture == null) {
            System.err.println("No Clip");
            return;
        }

        Dimension targetSize = this.computeTargetSize(aperture.width, aperture.height);
        g.drawImage(image, 0, 0, targetSize.width, targetSize.height, this);
    }

    @Override
    public Dimension getPreferredSize() {
        BufferedImage image = imageProvider.getImage();
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return super.getPreferredSize();
        }

        if (getParent() instanceof JViewport viewport) {
            int viewportWidth = Math.max(1, viewport.getWidth());
            int viewportHeight = Math.max(1, viewport.getHeight());
            return this.computeTargetSize(viewportWidth, viewportHeight);
        }

        return new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 64;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return paintFullWidthExclusion || this.isImageWiderThanTall();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return !paintFullWidthExclusion && !this.isImageWiderThanTall();
    }
}
