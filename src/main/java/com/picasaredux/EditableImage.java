package com.picasaredux;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

class EditableImage extends JPanel {

    final SortedSet<String> actionsPerformed = new TreeSet<>();
    BufferedImage image;
    File originalImageFile;
    int originalImageType;
    int netRotationDegrees = 0;
    boolean paintFullWidthExclusion = false;

    private static Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public void setImage(ImageFileInTree fileInTree) {
        originalImageFile = fileInTree.getUnderlying();

        try {
            image = ImageIO.read(originalImageFile);
            originalImageType = image.getType();

            this.repaint();
            this.revalidate();
        } catch (IOException e) {
            throw new RuntimeException("Could not load \"" + originalImageFile + "\". Reason: " + e.getMessage(), e);
        }
    }

    public void toggleRenderingMode(final boolean isSelected) {
        paintFullWidthExclusion = isSelected;
        this.repaint();
        this.revalidate();
    }

    public void rotateClockwise() {
        AffineTransform tx = AffineTransform.getRotateInstance(Math.PI / 2, image.getWidth() / 2d, image.getHeight() / 2d);
        double offset = (image.getWidth() - image.getHeight()) / 2d;
        tx.translate(offset, offset);
        applyTransformation(tx, image.getHeight(), image.getWidth()); // h & w reversed
        netRotationDegrees += 90;
        if (netRotationDegrees != 0) {
            actionsPerformed.add("rotated");
        } else {
            actionsPerformed.remove("rotated");
        }
    }

    public void rotateAnticlockwise() {
        AffineTransform tx = AffineTransform.getRotateInstance(-Math.PI / 2, image.getWidth() / 2d, image.getHeight() / 2d);
        double offset = (image.getWidth() - image.getHeight()) / 2d;
        tx.translate(-offset, -offset);
        applyTransformation(tx, image.getHeight(), image.getWidth()); // h & w reversed
        netRotationDegrees -= 90;
        if (netRotationDegrees != 0) {
            actionsPerformed.add("rotated");
        } else {
            actionsPerformed.remove("rotated");
        }
    }

    public void mirror() {
        AffineTransform tx = AffineTransform.getScaleInstance(-1d, 1d);
        tx.translate(-image.getWidth(), 0);
        applyTransformation(tx, image.getWidth(), image.getHeight());
        toggleActionPerformed("mirrored");
    }

    public void flip() {
        AffineTransform tx = AffineTransform.getScaleInstance(1d, -1d);
        tx.translate(0, -image.getHeight());
        applyTransformation(tx, image.getWidth(), image.getHeight());
        toggleActionPerformed("flipped");
    }

    public void showMetadata() {
        float aspectRatio = (float) image.getWidth() / (float) image.getHeight();
        System.out.println("Aspect ratio: " + aspectRatio);
    }

    public ImageFileInTree saveCopy() {
        File fileCopy = new File(getFileToCopyInto());
        try {
            boolean success = ImageIO.write(image, "png", fileCopy);
            if (success) {
                return new ImageFileInTree(fileCopy);
            } else {
                throw new RuntimeException("Could not save copy to: " + fileCopy.getAbsolutePath() + " type: " + originalImageType);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save copy to: " + fileCopy.getAbsolutePath(), e);
        }
    }

    private void toggleActionPerformed(String item) {
        if (actionsPerformed.contains(item)) {
            actionsPerformed.remove(item);
        } else {
            actionsPerformed.add(item);
        }
    }

    private void applyTransformation(AffineTransform transformation, int width, int height) {
        final AffineTransformOp op = new AffineTransformOp(transformation, AffineTransformOp.TYPE_BILINEAR);
        image = op.filter(image, new BufferedImage(width, height, originalImageType)); // new BufferedImage ensures type is maintained
        super.repaint();
        super.revalidate();
    }

    private String generateActionsPerformedSummary() {
        String actionsPerformedSummary = String.join(" ", actionsPerformed).trim();
        if (actionsPerformedSummary.isBlank()) {
            return " copied at " + System.currentTimeMillis();
        } else {
            return "_" + actionsPerformedSummary;
        }
    }

    private String getFileToCopyInto() {
        String originalFilePath = originalImageFile.getAbsolutePath();
        Optional<String> originalFileExtension = getFileExtension(originalImageFile.getName());
        if (originalFileExtension.isPresent()) {
            String fileExtension = "." + originalFileExtension.get();
            return originalFilePath.replace(fileExtension, generateActionsPerformedSummary() + fileExtension);
        } else {
            return originalFilePath + generateActionsPerformedSummary();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {
            Rectangle clip = g.getClipBounds();
            if (clip != null) {
                int currentWidth = clip.width;
                int currentHeight = clip.height;
                int targetWidth = currentWidth;
                int targetHeight = currentHeight;
                boolean paintFullWidth = image.getWidth() >= image.getHeight();

                if (paintFullWidth || paintFullWidthExclusion) {
                    float aspectRatio = (float) image.getHeight() / (float) image.getWidth();
                    targetHeight = Float.valueOf(currentWidth * aspectRatio).intValue();
                } else {
                    float aspectRatio = (float) image.getWidth() / (float) image.getHeight();
                    targetWidth = Float.valueOf(currentHeight * aspectRatio).intValue();
                }
                Dimension d = new Dimension(targetWidth, targetHeight);
                if (!this.getSize().equals(d)) {
                    this.setPreferredSize(d);
                }
                g.drawImage(image, 0, 0, targetWidth, targetHeight, this);
            } else {
                System.err.println("No Clip");
            }
        }
    }
}
