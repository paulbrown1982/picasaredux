package com.picasaredux.model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFileInTree extends FileInTree {

    private static final FaceDetector FACE_DETECTOR = OpenCvFaceDetector.INSTANCE;

    private Long hash;
    private Boolean containsFace;

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
        BufferedImage image = loadImage();
        if (image == null) return 0;
        int height = image.getHeight();
        image.flush();
        return height;
    }

    public int getWidth() {
        BufferedImage image = loadImage();
        if (image == null) return 0;
        int width = image.getWidth();
        image.flush();
        return width;
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

    private BufferedImage loadImage() {
        try {
            return ImageIO.read(getUnderlying());
        } catch (IOException e) {
            throw new RuntimeException("Could not load \"" + this + "\". Reason: " + e.getMessage(), e);
        }
    }

}
