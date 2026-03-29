package com.picasaredux;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

final class Utils
{
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final DateTimeFormatter UK_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma")
            .withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault());

    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");
    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("0.00");

    static String ukDateFormat(Instant instant) {
        return UK_DATE_FORMAT.format(instant);
    }

    static String exifToUkDateFormat(String dateToParse) {
        if (dateToParse == null || dateToParse.isEmpty()) {
            return dateToParse;
        }
        return UK_DATE_FORMAT.format(EXIF_DATE_FORMAT.parse(dateToParse));
    }

    static String bytesPrinter(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return ONE_DECIMAL.format((double) bytes / 1024d) + " KB";
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return ONE_DECIMAL.format((double) bytes / (1024d * 1024d)) + " MB";
        }
        return ONE_DECIMAL.format((double) bytes / (1024d * 1024d * 1024d)) + " GB";
    }

    static String oneDecimal(double number) {
        return ONE_DECIMAL.format(number);
    }

    static String twoDecimals(double number) {
        return TWO_DECIMALS.format(number);
    }

    static Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    static boolean isImage(File file) {
        if (file.isDirectory()) return false;
        if (file.getName().startsWith(".")) return false;

        try {
            String fileType = Files.probeContentType(file.toPath());
            return (fileType != null && fileType.contains("image"));
        } catch (IOException ioe) {
            System.err.println("Error probing Content Type of file (" + file.getAbsolutePath() + "): " + ioe.getMessage());
            return false;
        }
    }

   static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String imageTypeLabel(int imageType) {
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

    static String colorModelSummary(ColorModel colorModel) {
        return colorModel.getNumColorComponents() + " channels, "
                + colorModel.getPixelSize() + "-bit, "
                + (colorModel.hasAlpha() ? "with alpha" : "opaque");
    }

    static String reducedAspectRatio(int width, int height) {
        int gcd = gcd(width, height);
        return (width / gcd) + ":" + (height / gcd);
    }

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
}
