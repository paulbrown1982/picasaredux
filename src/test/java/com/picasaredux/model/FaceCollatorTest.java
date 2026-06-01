package com.picasaredux.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceCollatorTest {

    @Test
    void stockImagesClustersDoNotRepeatTheSameImageAcrossGroups() throws IOException {
        Path stockImages = Path.of("stock_images");
        assertTrue(Files.isDirectory(stockImages), "Expected stock_images fixture directory to exist");

        List<ImageFileInTree> images = Files.walk(stockImages)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(Utils::isImage)
                .map(ImageFileInTree::new)
                .toList();

        FaceCollator collator = new FaceCollator();
        List<List<ImageFileInTree>> clusters = collator.cluster(images);

        assertFalse(clusters.isEmpty(), "Expected at least one face cluster in stock_images");

        Set<String> seen = new HashSet<>();
        Set<String> repeats = new HashSet<>();
        for (List<ImageFileInTree> cluster : clusters) {
            for (ImageFileInTree image : cluster) {
                String path = image.getAbsoluteFilePath();
                if (!seen.add(path)) {
                    repeats.add(path);
                }
            }
        }

        assertTrue(repeats.isEmpty(), "Repeated images across face clusters: " + repeats);
    }
}
