package com.picasaredux.view;

import com.picasaredux.model.FileTree;
import com.picasaredux.model.ImageFileInTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlbumTest {

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
    void showsAllImagesByDefaultAndOnlyDuplicatesWhenEnabled() throws IOException {
        Path album = Files.createDirectory(tempDir.resolve("album"));
        Path folder = Files.createDirectory(album.resolve("folder"));

        Path duplicateA = folder.resolve("dup-a.png");
        Path duplicateB = folder.resolve("dup-b.png");
        Path unique = folder.resolve("unique.png");

        writePng(duplicateA);
        Files.copy(duplicateA, duplicateB);
        writeDifferentPng(unique);

        Album view = new Album();
        view.setAlbum(album.toString());

        assertEquals(List.of("dup-a.png", "dup-b.png", "unique.png"), collectImageNames(view));

        view.setFilterMode(Album.FilterMode.DUPLICATES);
        assertEquals(List.of("dup-a.png", "dup-b.png"), collectImageNames(view));
    }

    @Test
    void showsOnlyFaceImagesWhenFaceFilterEnabled() throws IOException {
        Path album = Files.createDirectory(tempDir.resolve("album"));
        Path folder = Files.createDirectory(album.resolve("folder"));
        Path faceImage = folder.resolve("face-01.png");
        Path nonFaceImage = folder.resolve("landscape.png");
        writePng(faceImage);
        writePng(nonFaceImage);

        FileTree model = FileTree.withProviders(
                _ -> 0L,
                image -> image.getFileName().contains("face"));
        Album view = new Album(model);
        view.setAlbum(album.toString());

        assertEquals(List.of("face-01.png", "landscape.png"), collectImageNames(view));

        view.setFilterMode(Album.FilterMode.FACES);
        assertEquals(List.of("face-01.png"), collectImageNames(view));
    }

    private static List<String> collectImageNames(Album view) {
        Object root = view.getTree().getModel().getRoot();
        if (!(root instanceof DefaultMutableTreeNode dtm)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        collectImageNames(dtm, names);
        return names.stream().sorted().toList();
    }

    private static void collectImageNames(TreeNode node, List<String> names) {
        if (node instanceof DefaultMutableTreeNode dtm && dtm.getUserObject() instanceof ImageFileInTree image) {
            names.add(image.getFileName());
        }
        Enumeration<? extends TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            collectImageNames(children.nextElement(), names);
        }
    }

    private static void writePng(Path path) throws IOException {
        Files.write(path, ONE_PIXEL_PNG);
    }

    private static void writeDifferentPng(Path path) throws IOException {
        byte[] differentLength = new byte[ONE_PIXEL_PNG.length + 1];
        System.arraycopy(ONE_PIXEL_PNG, 0, differentLength, 0, ONE_PIXEL_PNG.length);
        differentLength[differentLength.length - 1] = 1;
        Files.write(path, differentLength);
    }
}

