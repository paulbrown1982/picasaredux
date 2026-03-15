package com.picasaredux;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

    private static Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

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

    void showMetadata() {
        float aspectRatio = (float) image.getWidth() / (float) image.getHeight();
        System.out.println("Aspect ratio: " + aspectRatio);
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
        Optional<String> originalFileExtension = getFileExtension(originalImageFile.getName());
        if (originalFileExtension.isPresent()) {
            String fileExtension = "." + originalFileExtension.get();
            return originalFilePath.replace(fileExtension, generateActionsPerformedSummary() + fileExtension);
        } else {
            return originalFilePath + generateActionsPerformedSummary();
        }
    }
}
