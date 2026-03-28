package com.picasaredux;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

class EditableImage extends UnderlyingSwingComponent implements ImageProvider {

    private final ImageCanvas canvas;

    private final SortedSet<String> actionsPerformed = new TreeSet<>();
    private BufferedImage image;
    private File originalImageFile;
    private int originalImageType;
    private int netRotationDegrees = 0;

    EditableImage() {
        canvas = new ImageCanvas(this);
        setUnderlyingComponent(canvas);
    }

    @Override
    public BufferedImage getImage() {
        return image;
    }

    void setImage(ImageFileInTree fileInTree) {
        originalImageFile = fileInTree.getUnderlying();

        try {
            image = ImageIO.read(originalImageFile);
            originalImageType = image.getType();

            canvas.repaint();
            canvas.revalidate();
        } catch (IOException e) {
            throw new RuntimeException("Could not load \"" + originalImageFile + "\". Reason: " + e.getMessage(), e);
        }
    }

    void toggleRenderingMode(final boolean isSelected) {
        canvas.toggleRenderingMode(isSelected);
        canvas.repaint();
        canvas.revalidate();
    }

    void rotateClockwise() {
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

    void rotateAnticlockwise() {
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

    void mirror() {
        AffineTransform tx = AffineTransform.getScaleInstance(-1d, 1d);
        tx.translate(-image.getWidth(), 0);
        applyTransformation(tx, image.getWidth(), image.getHeight());
        toggleActionPerformed("mirrored");
    }

    void flip() {
        AffineTransform tx = AffineTransform.getScaleInstance(1d, -1d);
        tx.translate(0, -image.getHeight());
        applyTransformation(tx, image.getWidth(), image.getHeight());
        toggleActionPerformed("flipped");
    }

    void showMetadata(JLabel label) {
        if (image == null || originalImageFile == null) {
            label.setText("No image loaded");
            return;
        }

        double width = image.getWidth();
        double height = image.getHeight();
        double aspectRatio = width / height;
        double megapixels = (width * height) / 1_000_000d;
        String format = Utils.getFileExtension(originalImageFile.getName()).orElse("Unknown");
        long fileSizeBytes = originalImageFile.length();
        String orientation = width == height ? "Square" : (width > height ? "Landscape" : "Portrait");
        String modified = Utils.ukDateFormat(Instant.ofEpochMilli(originalImageFile.lastModified()));

        label.setText("<html>"
                + "<table border='1' cellspacing='0' cellpadding='4'>"
                + "<th><td colpsan=\"2\"><b>Metadata</b></td></th>"
                + "<tr><td>Dimensions:</td><td>" + image.getWidth() + " × " + image.getHeight() + " px</td></tr>"
                + "<tr><td>Megapixels:</td><td>" + Utils.oneDecimal(megapixels) + " MP</td></tr>"
                + "<tr><td>File size:</td><td>" + Utils.bytesPrinter(fileSizeBytes) + "</td></tr>"
                + "<tr><td>Format:</td><td>" + format.toUpperCase() + "</td></tr>"
                + "<tr><td>Orientation:</td><td>" + orientation + "</td></tr>"
                + "<tr><td>Modified:</td><td>" + modified + "<</td></tr>"
                + "<tr><td>Aspect ratio:</td><td>" + Utils.twoDecimals(aspectRatio) + "</td></tr>"
                + "</table>"
                + "</html>");
    }

    ImageFileInTree saveCopy() {
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
        canvas.repaint();
        canvas.revalidate();
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
        Optional<String> originalFileExtension = Utils.getFileExtension(originalImageFile.getName());
        if (originalFileExtension.isPresent()) {
            String fileExtension = "." + originalFileExtension.get();
            return originalFilePath.replace(fileExtension, generateActionsPerformedSummary() + fileExtension);
        } else {
            return originalFilePath + generateActionsPerformedSummary();
        }
    }
}
