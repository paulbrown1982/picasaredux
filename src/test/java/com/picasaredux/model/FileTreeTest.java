package com.picasaredux.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileTreeTest {

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
    void rebuildFromRootPicksUpNewFiles() throws IOException {
        Path album = Files.createDirectory(tempDir.resolve("album"));
        Path first = album.resolve("first.png");
        Path second = album.resolve("second.png");

        writePng(first);

        FileTree model = new FileTree();
        model.setAlbum(album.toString());
        assertEquals(List.of("first.png"), collectImageNames(model.getRoot()));

        writePng(second);
        model.rebuildFromRoot();
        assertEquals(List.of("first.png", "second.png"), collectImageNames(model.getRoot()));
    }

    private static void writePng(Path path) throws IOException {
        Files.write(path, ONE_PIXEL_PNG);
    }

    private static List<String> collectImageNames(FileTree.Node root) {
        assertNotNull(root);
        List<String> names = new ArrayList<>();
        collectImageNames(root, names);
        return names.stream().sorted().toList();
    }

    private static void collectImageNames(FileTree.Node node, List<String> names) {
        if (node.fileInTree() instanceof ImageFileInTree image) {
            names.add(image.getFileName());
        }
        node.children().forEach(child -> collectImageNames(child, names));
    }
}
