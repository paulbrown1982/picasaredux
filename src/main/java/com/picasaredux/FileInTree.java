package com.picasaredux;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.CRC32;

abstract class FileInTree {

    final static DateTimeFormatter shortDf = createDTF("dd MMM yy");

    final static DateTimeFormatter timeDf = createDTF("h:mma");

    final static DateTimeFormatter dayTimeDf = createDTF("dd MMM, h:mm a");

    final static DateTimeFormatter isoDf = createDTF("yyyy-MM-dd");

    final static DateTimeFormatter yearDayDf = createDTF("yyyy-MM");

    private static final ThreadLocal<CRC32> CRC32_HASH = ThreadLocal.withInitial(CRC32::new);
    private static final ThreadLocal<byte[]> DIGEST_BUFFER = ThreadLocal.withInitial(() -> new byte[256 * 1024]);

    final File file;

    Long fileSize = 0L;

    private String creationTime;


    FileInTree(File f) {
        file = f;
        getAttributes(file).ifPresent(attributes -> {
            fileSize = attributes.size();
            creationTime = establishCreationTime(attributes);
        });
    }

    static Long getDigest(File f) {
        try {
            CRC32 crc32 = CRC32_HASH.get();
            crc32.reset();
            try (InputStream fi = Files.newInputStream(f.toPath())) {
                byte[] buffer = DIGEST_BUFFER.get();
                int read;
                while ((read = fi.read(buffer)) != -1) {
                    crc32.update(buffer, 0, read);
                }
            }
            return crc32.getValue();
        } catch (IOException e) {
            throw new RuntimeException("cannot read file " + f.getAbsolutePath(), e);
        }
    }

    static DateTimeFormatter createDTF(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
    }

    protected static String bytesPrinter(Long bytes) {
        return String.format("%,d", Math.round(bytes / 100000f)) + " MB";
    }

    File getUnderlying() {
        return file;
    }

    String getAbsoluteFilePath() {
        return file.getAbsolutePath();
    }

    @SuppressWarnings("SpellCheckingInspection")
    private Optional<BasicFileAttributes> getAttributes(File file) {
        BasicFileAttributeView bfav = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
        BasicFileAttributes attributes = null;
        try {
            attributes = bfav.readAttributes();
        } catch (IOException ioe) {
            System.err.println("Error reading file attributes: " + ioe.getMessage());
        }
        return Optional.ofNullable(attributes);
    }

    private String establishCreationTime(BasicFileAttributes attributes) {
        Instant fileCreationTime = attributes.creationTime().toInstant();
        File parentFile = file.getParentFile();
        String parentFileName = parentFile.getName();
        String ct;
        if (parentFileName.startsWith(isoDf.format(fileCreationTime))) {
            ct = timeDf.format(fileCreationTime);
        } else if ((parentFile.getParentFile().getName() + "-" + parentFileName).startsWith(yearDayDf.format(fileCreationTime))) {
            ct = dayTimeDf.format(fileCreationTime);
        } else {
            ct = shortDf.format(fileCreationTime);
        }
        return ct;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        FileInTree that = (FileInTree) o;
        return file.equals(that.file) && fileSize.equals(that.fileSize) && creationTime.equals(that.creationTime);
    }

    @Override
    public int hashCode() {
        int result = file.hashCode();
        result = 31 * result + fileSize.hashCode();
        result = 31 * result + creationTime.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (creationTime != null && fileSize != null) {
            return file.getName() + " [" + creationTime + "; " + bytesPrinter(fileSize) + "]";
        } else {
            return file.getName();
        }
    }
}
