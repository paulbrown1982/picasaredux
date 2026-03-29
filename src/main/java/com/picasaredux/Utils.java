package com.picasaredux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Optional;

final class Utils
{
    private static final DateTimeFormatter UK_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma")
            .withLocale(Locale.UK)
            .withZone(ZoneId.systemDefault());

    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");

    static String ukDateFormat(TemporalAccessor ta) {
        if (ta == null) {
            return "";
        }
        return UK_DATE_FORMAT.format(ta);
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
}
