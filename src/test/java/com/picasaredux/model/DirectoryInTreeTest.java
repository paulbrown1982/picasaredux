package com.picasaredux.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryInTreeTest {

    private static final byte[] ONE_PIXEL_PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x60, 0x00, 0x00, 0x00,
            0x02, 0x00, 0x01, (byte) 0xE5, 0x27, (byte) 0xD4, (byte) 0xA2,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @TempDir
    Path tempDir;

    @Test
    void listsOnlyDirectChildrenAndFormatsRecursiveSummary() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("album"));
        Path nested = Files.createDirectory(root.resolve("nested"));
        Path directImage = root.resolve("direct.png");
        Path nestedImage = nested.resolve("nested.png");

        writePng(directImage);
        writeSameLengthPngWithDifferentContent(nestedImage);
        Files.writeString(root.resolve("notes.txt"), "not an image");

        DirectoryInTree directory = new DirectoryInTree(root.toFile(), new HashMap<>());

        assertEquals(List.of("nested"), directory.listChildFolders(false).stream().map(FileInTree::getFileName).toList());
        assertEquals(List.of("direct.png"), directory.listChildImages(false).stream().map(FileInTree::getFileName).toList());
        assertTrue(directory.isNotEmpty());
        assertFalse(directory.containsDuplicateFiles());

        long expectedSize = Files.size(directImage) + Files.size(nestedImage);
        String summary = directory.toString();
        assertTrue(summary.contains("2 images"));
        assertTrue(summary.contains(Utils.bytesPrinter(expectedSize)));
    }

    @Test
    void emptyDirectoryIsReportedAsEmpty() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("empty"));

        DirectoryInTree directory = new DirectoryInTree(root.toFile(), new HashMap<>());

        assertTrue(directory.listChildFolders(false).isEmpty());
        assertTrue(directory.listChildImages(false).isEmpty());
        assertFalse(directory.isNotEmpty());
    }

    @Test
    void duplicateCountUsesHashesNotOnlyFileSize() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("album"));
        Path first = root.resolve("first.png");
        Path same = root.resolve("same.png");
        Path sameSizeDifferentBytes = root.resolve("different-bytes-same-size.png");

        writePng(first);
        Files.copy(first, same);
        writeSameLengthPngWithDifferentContent(sameSizeDifferentBytes);

        DirectoryInTree directory = new DirectoryInTree(root.toFile(), new HashMap<>());
        String summary = directory.toString();

        assertTrue(directory.containsDuplicateFiles());
        assertTrue(summary.contains("3 images"));
        assertTrue(summary.contains("1 dupes"));
    }

    @Test
    void equalSizeFilesWithDifferentHashesAreNotCountedAsDuplicates() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("album"));
        Path first = root.resolve("first.png");
        Path second = root.resolve("second.png");

        writePng(first);
        writeSameLengthPngWithDifferentContent(second);

        DirectoryInTree directory = new DirectoryInTree(root.toFile(), new HashMap<>());

        assertFalse(directory.containsDuplicateFiles());
        assertFalse(directory.toString().contains("dupes"));
    }

    @Test
    void flushCacheRefreshesChildrenAfterFilesystemChanges() throws IOException {
        Path root = Files.createDirectory(tempDir.resolve("album"));
        Path initial = root.resolve("first.png");
        writePng(initial);

        DirectoryInTree directory = new DirectoryInTree(root.toFile(), new HashMap<>());
        assertEquals(1, directory.listChildImages(false).size());

        writePng(root.resolve("second.png"));
        assertEquals(1, directory.listChildImages(false).size());
        assertEquals(2, directory.listChildImages(true).size());
    }

    private static void writePng(Path path) throws IOException {
        Files.write(path, ONE_PIXEL_PNG);
    }

    private static void writeSameLengthPngWithDifferentContent(Path path) throws IOException {
        byte[] different = ONE_PIXEL_PNG.clone();
        different[different.length - 1] = 0;
        Files.write(path, different);
    }
}
