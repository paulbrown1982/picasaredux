package com.picasaredux;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class ImageCanvas extends JPanel implements Scrollable {
    final PaintableImage imageToPaint;

    ImageCanvas(PaintableImage imageToPaint) {
        this.imageToPaint = imageToPaint;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage image = imageToPaint.getImage();
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            System.err.println("No Image");
            return;
        }

        Rectangle aperture = g.getClipBounds();
        if (aperture == null) {
            System.err.println("No Clip");
            return;
        }

        Dimension targetSize = imageToPaint.computeTargetSize(aperture.width, aperture.height);
        g.drawImage(image, 0, 0, targetSize.width, targetSize.height, this);
    }

    @Override
    public Dimension getPreferredSize() {
        BufferedImage image = imageToPaint.getImage();
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return super.getPreferredSize();
        }

        if (getParent() instanceof JViewport viewport) {
            int viewportWidth = Math.max(1, viewport.getWidth());
            int viewportHeight = Math.max(1, viewport.getHeight());
            return imageToPaint.computeTargetSize(viewportWidth, viewportHeight);
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
        return imageToPaint.isPaintFullWidthExclusionEnabled() || imageToPaint.isImageWiderThanTall();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return !imageToPaint.isPaintFullWidthExclusionEnabled() && !imageToPaint.isImageWiderThanTall();
    }
}
