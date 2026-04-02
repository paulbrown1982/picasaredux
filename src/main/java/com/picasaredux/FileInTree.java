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

    private static final DateTimeFormatter shortDf = createDTF("dd MMM yy");

    private static final DateTimeFormatter timeDf = createDTF("h:mma");

    private static final DateTimeFormatter dayTimeDf = createDTF("dd MMM, h:mm a");

    private static final DateTimeFormatter isoDf = createDTF("yyyy-MM-dd");

    private static final DateTimeFormatter yearDayDf = createDTF("yyyy-MM");

    private static final ThreadLocal<CRC32> CRC32_HASH = ThreadLocal.withInitial(CRC32::new);

    private static final ThreadLocal<byte[]> DIGEST_BUFFER = ThreadLocal.withInitial(() -> new byte[256 * 1024]);

    final File file;

    long fileSize = 0L;

    String creationTime;

    final String fileName;

    FileInTree(File f) {
        file = f;
        fileName = f.getName();
        getAttributes(file).ifPresent(attributes -> {
            fileSize = attributes.size();
            creationTime = establishCreationTime(attributes);
        });
    }

    static long getDigest(File f) {
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
        return file.equals(that.file) && fileSize == that.fileSize && creationTime.equals(that.creationTime);
    }

    @Override
    public int hashCode() {
        int result = file.hashCode();
        result = 31 * result + Long.hashCode(fileSize);
        result = 31 * result + creationTime.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (creationTime != null) {
            return fileName + " [" + creationTime + "; " + Utils.bytesPrinter(fileSize) + "]";
        } else {
            return fileName;
        }
    }
}
