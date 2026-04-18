package com.picasaredux.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class FileInTreeTest {

    @TempDir
    Path tempDir;

    @Test
    void getDigest_emptyFileReturnsZero() throws IOException {
        Path path = tempDir.resolve("empty.bin");
        Files.createFile(path);

        assertEquals(0L, FileInTree.getDigest(path.toFile()));
    }

    @Test
    void getDigest_matchesIndependentCrc32() throws IOException {
        byte[] data = {0x01, 0x02, 0x03, (byte) 0xff};
        Path path = tempDir.resolve("data.bin");
        Files.write(path, data);

        CRC32 expected = new CRC32();
        expected.update(data);

        assertEquals(expected.getValue(), FileInTree.getDigest(path.toFile()));
    }

    @Test
    void getDigest_sameContentSameValue() throws IOException {
        byte[] data = "duplicate-payload".getBytes();
        Path a = tempDir.resolve("a.bin");
        Path b = tempDir.resolve("b.bin");
        Files.write(a, data);
        Files.write(b, data);

        assertEquals(FileInTree.getDigest(a.toFile()), FileInTree.getDigest(b.toFile()));
    }

    @Test
    void getDigest_wrapsIOExceptionInRuntimeException() {
        File missing = tempDir.resolve("missing-no-such-file.bin").toFile();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> FileInTree.getDigest(missing));

        assertTrue(ex.getMessage().contains("cannot read file"));
        assertNotNull(ex.getCause());
        assertInstanceOf(IOException.class, ex.getCause());
    }

    @Test
    void getAttributes_populatesSizeAndCreationTimeWhenReadable() throws IOException {
        Path path = tempDir.resolve("sized.txt");
        byte[] payload = "hello-attributes".getBytes();
        Files.write(path, payload);

        ImageFileInTree fit = new ImageFileInTree(path.toFile());

        assertEquals(payload.length, fit.fileSize);
        assertNotNull(fit.creationTime);
    }

    @Test
    void getAttributes_leavesDefaultsWhenFileMissing() {
        File missing = tempDir.resolve("not-there.jpg").toFile();
        ImageFileInTree fit = new ImageFileInTree(missing);

        assertEquals(0L, fit.fileSize);
        assertNull(fit.creationTime);
    }

    @Test
    void establishCreationTime_usesTimeOnlyWhenParentMatchesIsoDate() throws IOException {
        ZonedDateTime zdt = ZonedDateTime.of(2024, 6, 10, 15, 30, 0, 0, ZoneId.systemDefault());
        Instant instant = zdt.toInstant();
        String isoFolder = FileInTree.createDTF("yyyy-MM-dd").format(instant);
        Path file = tempDir.resolve(isoFolder).resolve("photo.jpg");
        Files.createDirectories(file.getParent());
        Files.createFile(file);

        Assumptions.assumeTrue(setCreationTime(file, instant));

        ImageFileInTree fit = new ImageFileInTree(file.toFile());
        String expected = FileInTree.createDTF("h:mma").format(instant);

        assertEquals(expected, fit.creationTime);
    }

    @Test
    void formatCreationTime_usesDayTimeWhenGrandparentParentMatchesYearMonth() {
        ZonedDateTime zdt = ZonedDateTime.of(2024, 3, 20, 9, 5, 0, 0, ZoneId.systemDefault());
        Instant instant = zdt.toInstant();
        File parent = tempDir.resolve(FileInTree.createDTF("yyyy-MM").format(instant))
                .resolve("day20")
                .toFile();
        String expected = FileInTree.createDTF("dd MMM, h:mm a").format(instant);

        assertEquals(expected, FileInTree.formatCreationTime(parent, instant));
    }

    @Test
    void formatCreationTime_usesShortDateOtherwise() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 12, 25, 11, 0, 0, 0, ZoneId.systemDefault());
        Instant instant = zdt.toInstant();
        File parent = tempDir.resolve("misc-album").toFile();
        String expected = FileInTree.createDTF("dd MMM yy").format(instant);

        assertEquals(expected, FileInTree.formatCreationTime(parent, instant));
    }

    @Test
    void formatCreationTime_usesShortDateWhenNoParentAvailable() {
        ZonedDateTime zdt = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        Instant instant = zdt.toInstant();
        String expected = FileInTree.createDTF("dd MMM yy").format(instant);

        assertEquals(expected, FileInTree.formatCreationTime(null, instant));
    }

    @Test
    void equalsAndHashCode_handleNullCreationTime() {
        File missing = tempDir.resolve("missing").toFile();
        ImageFileInTree left = new ImageFileInTree(missing);
        ImageFileInTree right = new ImageFileInTree(missing);

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    private static boolean setCreationTime(Path path, Instant instant) {
        BasicFileAttributeView view = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        if (view == null) {
            return false;
        }
        FileTime ft = FileTime.from(instant);
        try {
            view.setTimes(ft, ft, ft);
        } catch (IOException e) {
            return false;
        }
        try {
            BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class);
            return after.creationTime().toInstant().equals(instant);
        } catch (IOException e) {
            return false;
        }
    }
}
