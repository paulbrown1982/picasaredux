package com.picasaredux.service;

import com.picasaredux.model.ImageFileInTree;
import com.picasaredux.model.Utils;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCvFaceCollatorTest {
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

        OpenCvFaceCollator collator = new OpenCvFaceCollator();
        List<List<ImageFileInTree>> clusters = collator.cluster(images);

        assertEquals(3, clusters.size(), "Expected 3 face clusters in stock_images");

        Set<String> seen = new HashSet<>();
        Set<String> repeats = new HashSet<>();
        List<ImageFileInTree> overallImages = new ArrayList<>();

        for (List<ImageFileInTree> cluster : clusters) {
            for (ImageFileInTree image : cluster) {
                overallImages.add(image);
                String path = image.getAbsoluteFilePath();
                if (!seen.add(path)) {
                    repeats.add(path);
                }
            }
        }

        assertTrue(repeats.isEmpty(), "Repeated images across face clusters: " + repeats);

        assertEquals(5, overallImages.size(), "There should only be 5 faces overall in stock_images");

    }

    @Test
    void juneSecondStockImagesStayInTheSameFaceCluster() throws IOException {
        Path stockImages = Path.of("stock_images/2026-06-02");
        assertTrue(Files.isDirectory(stockImages), "Expected stock_images/2026-06-02 fixture directory to exist");

        List<ImageFileInTree> images = Files.list(stockImages)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(Utils::isImage)
                .map(ImageFileInTree::new)
                .toList();

        assertEquals(2, images.size(), "Expected exactly two stock images in the June 2 fixture");

        List<List<ImageFileInTree>> clusters = new OpenCvFaceCollator().cluster(images);
        Set<String> expected = images.stream()
                .map(ImageFileInTree::getAbsoluteFilePath)
                .collect(Collectors.toSet());

        boolean groupedTogether = clusters.stream().anyMatch(cluster ->
                cluster.stream()
                        .map(ImageFileInTree::getAbsoluteFilePath)
                        .collect(Collectors.toSet())
                        .containsAll(expected));

        String clusterSummary = clusters.stream()
                .map(cluster -> cluster.stream()
                        .map(ImageFileInTree::getAbsoluteFilePath)
                        .collect(Collectors.joining(", ", "[", "]")))
                .collect(Collectors.joining("; "));

        System.out.println(clusterSummary);

        assertTrue(groupedTogether, "Expected both June 2 images to land in the same face cluster. Clusters: " + clusterSummary);
    }
}
