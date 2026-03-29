package com.picasaredux;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

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

    String getMetadataHTML() {
        if (image == null || originalImageFile == null) {
            return "No image loaded";
        }

        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = (double) width / (double) height;
        double megapixels = (width * height) / 1_000_000d;
        String format = Utils.getFileExtension(originalImageFile.getName()).orElse("Unknown");
        long fileSizeBytes = originalImageFile.length();
        String orientation = width == height ? "Square" : (width > height ? "Landscape" : "Portrait");
        String modified = Utils.ukDateFormat(Instant.ofEpochMilli(originalImageFile.lastModified()));

        ExifData.ExifMetadataSummary exif = ExifData.readExifMetadata(originalImageFile);

        StringBuilder html = new StringBuilder("<html>");
        html.append("<table border='1' cellspacing='0' cellpadding='4'>");
        html.append("<tr><th colspan=\"2\"><b>File Metadata</b></th></tr>");
        appendRow(html, "Aspect ratio", Utils.twoDecimals(aspectRatio) + " (" + Utils.reducedAspectRatio(width, height) + ")");
        appendRow(html, "Color model", Utils.colorModelSummary(image.getColorModel()));
        appendRow(html, "Dimensions", width + " × " + height + " px");
        appendRow(html, "File size", Utils.bytesPrinter(fileSizeBytes) + " (" + fileSizeBytes + " bytes)");
        appendRow(html, "Filename", originalImageFile.getName());
        appendRow(html, "Format", format.toUpperCase());
        appendRow(html, "Has alpha", image.getColorModel().hasAlpha() ? "Yes" : "No");
        appendRow(html, "Megapixels", Utils.oneDecimal(megapixels) + " MP");
        appendRow(html, "Modified", modified);
        appendRow(html, "Orientation", orientation);
        appendRow(html, "Pixel type", Utils.imageTypeLabel(image.getType()));
        appendRow(html, "Transforms", currentTransformSummary());
        if (exif.error() != null) {
            appendRow(html, "EXIF status", "Could not parse metadata: " + exif.error());
        } else if (exif.hasAnyValues()) {
            html.append("<tr><th colspan=\"2\"><b>EXIF Data</b></th></tr>");
            appendRowIfPresent(html, "Aperture", exif.aperture());
            appendRowIfPresent(html, "Camera", exif.camera());
            appendRowIfPresent(html, "Color space", exif.colorSpace());
            appendRowIfPresent(html, "Focal length", exif.focalLength());
            appendRowIfPresent(html, "GPS", exif.gps());
            appendRowIfPresent(html, "ICC profile", exif.iccProfile());
            appendRowIfPresent(html, "ISO", exif.iso());
            appendRowIfPresent(html, "Lens", exif.lens());
            appendRowIfPresent(html, "Orientation", exif.orientation());
            appendRowIfPresent(html, "Photo taken", Utils.exifToUkDateFormat(exif.dateTaken()));
            appendRowIfPresent(html, "Shutter", exif.shutter());
        } else {
            appendRow(html, "EXIF", "No camera/location metadata found");
        }

        html.append("</table></html>");

        return html.toString();
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

    private String currentTransformSummary() {
        List<String> transforms = new ArrayList<>();
        if (netRotationDegrees != 0 && netRotationDegrees != 360) {
            int normalised = netRotationDegrees % 360;
            normalised = normalised < 0 ? normalised + 360 : normalised;
            transforms.add("Rotated " + normalised + "°");
        }
        if (actionsPerformed.contains("mirrored")) {
            transforms.add("Mirrored");
        }
        if (actionsPerformed.contains("flipped")) {
            transforms.add("Flipped");
        }
        return transforms.isEmpty() ? "None" : String.join(", ", transforms);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void appendRow(StringBuilder html, String key, String value) {
        html.append("<tr><td>")
                .append(escapeHtml(key))
                .append(":</td><td>")
                .append(escapeHtml(value))
                .append("</td></tr>");
    }

    private static void appendRowIfPresent(StringBuilder html, String key, String value) {
        if (value != null && !value.isBlank()) {
            appendRow(html, key, value);
        }
    }
}
