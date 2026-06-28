package com.picasaredux.model;

import com.picasaredux.service.FaceDetector;
import com.picasaredux.service.OpenCvFaceDetector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ImageFileInTree extends FileInTree {

    private static final FaceDetector FACE_DETECTOR = new OpenCvFaceDetector();

    private Long hash;
    private Boolean containsFace;
    private Dimension dimension;

    public ImageFileInTree(File f) {
        super(f);
    }

    Long getHash() {
        if (hash == null) {
            hash = getDigest(file);
        }
        return hash;
    }

    boolean containsFace() {
        if (containsFace == null) {
            containsFace = FACE_DETECTOR.hasFace(file);
        }
        return containsFace;
    }

    public int getHeight() {
        if (dimension == null) {
            updateDimension();
        }
        if (dimension != null) {
            return dimension.height;
        }
        return 0;
    }

    public int getWidth() {
        if (dimension == null) {
            updateDimension();
        }
        if (dimension != null) {
            return dimension.width;
        }
        return 0;
    }

    public Image getScaledInstance(Dimension newSize) {
        BufferedImage source = loadImage();
        if (source == null) return null;
        int targetWidth = Math.max(1, newSize.width);
        int targetHeight = Math.max(1, newSize.height);
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return source;
        }
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        source.flush();
        return scaled;
    }

    private void updateDimension() {
        try (ImageInputStream input = ImageIO.createImageInputStream(getUnderlying())) {
            if (input == null) return;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return;

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                dimension = new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read dimensions for \"" + this + "\". Reason: " + e.getMessage(), e);
        }
    }

    private BufferedImage loadImage() {
        try {
            return ImageIO.read(getUnderlying());
        } catch (IOException e) {
            throw new RuntimeException("Could not load \"" + this + "\". Reason: " + e.getMessage(), e);
        }
    }

}
