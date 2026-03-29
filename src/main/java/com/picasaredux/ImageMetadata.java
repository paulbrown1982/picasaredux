package com.picasaredux;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;

public class ImageMetadata {

    record Summary(
            String fileName,
            int width,
            int height,
            double aspectRatio,
            double megapixels,
            String format,
            long fileSizeBytes,
            String orientation,
            String modified,
            ColorModel colourModel,
            int imageType) {

        private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("0.00");

        private static int gcd(int a, int b) {
            int x = Math.abs(a);
            int y = Math.abs(b);
            while (y != 0) {
                int tmp = x % y;
                x = y;
                y = tmp;
            }
            return x == 0 ? 1 : x;
        }

        private static String reducedAspectRatio(int width, int height) {
            int gcd = gcd(width, height);
            return (width / gcd) + ":" + (height / gcd);
        }

        String dimensions() {
            return width() + " × " + height() + " px";
        }

        String displayableAspectRatio() {
            return TWO_DECIMALS.format(aspectRatio) + " (" + reducedAspectRatio(width, height) + ")";
        }

        String colourModelSummary() {
            return colourModel.getNumColorComponents() + " channels, "
                    + colourModel.getPixelSize() + "-bit, "
                    + (colourModel.hasAlpha() ? "with alpha" : "opaque");
        }

        String imageTypeLabel() {
            return switch (imageType) {
                case BufferedImage.TYPE_3BYTE_BGR -> "TYPE_3BYTE_BGR";
                case BufferedImage.TYPE_4BYTE_ABGR -> "TYPE_4BYTE_ABGR";
                case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "TYPE_4BYTE_ABGR_PRE";
                case BufferedImage.TYPE_BYTE_BINARY -> "TYPE_BYTE_BINARY";
                case BufferedImage.TYPE_BYTE_GRAY -> "TYPE_BYTE_GRAY";
                case BufferedImage.TYPE_BYTE_INDEXED -> "TYPE_BYTE_INDEXED";
                case BufferedImage.TYPE_CUSTOM -> "TYPE_CUSTOM";
                case BufferedImage.TYPE_INT_ARGB -> "TYPE_INT_ARGB";
                case BufferedImage.TYPE_INT_ARGB_PRE -> "TYPE_INT_ARGB_PRE";
                case BufferedImage.TYPE_INT_BGR -> "TYPE_INT_BGR";
                case BufferedImage.TYPE_INT_RGB -> "TYPE_INT_RGB";
                case BufferedImage.TYPE_USHORT_555_RGB -> "TYPE_USHORT_555_RGB";
                case BufferedImage.TYPE_USHORT_565_RGB -> "TYPE_USHORT_565_RGB";
                case BufferedImage.TYPE_USHORT_GRAY -> "TYPE_USHORT_GRAY";
                default -> "TYPE_" + imageType;
            };
        }

        String fileSizeSummary() {
            return Utils.bytesPrinter(fileSizeBytes()) + " (" + fileSizeBytes() + " bytes)";
        }

        String megapixelsSummary() {
            return Utils.oneDecimal(megapixels()) + " MP";
        }

    }

    static Summary generateSummary(BufferedImage image, File imageFile) {
        String fileName = imageFile.getName();
        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = (double) width / (double) height;
        double megapixels = (width * height) / 1_000_000d;
        String format = Utils.getFileExtension(imageFile.getName()).orElse("Unknown");
        long fileSizeBytes = imageFile.length();
        String orientation = width == height ? "Square" : (width > height ? "Landscape" : "Portrait");
        String modified = Utils.ukDateFormat(Instant.ofEpochMilli(imageFile.lastModified()));
        ColorModel colourModel = image.getColorModel();
        int imageType = image.getType();

        return new Summary(
                fileName, width, height, aspectRatio, megapixels, format, fileSizeBytes, orientation, modified, colourModel, imageType
        );
    }
}
