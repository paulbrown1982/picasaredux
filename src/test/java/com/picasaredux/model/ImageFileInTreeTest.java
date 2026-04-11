package com.picasaredux.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeAll;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageFileInTreeTest {

    @BeforeAll
    static void enableHeadlessAwt() {
        System.setProperty("java.awt.headless", "true");
    }

    @TempDir
    Path tempDir;

    @Test
    void scalesImageToRequestedSize() throws IOException {
        Path imagePath = tempDir.resolve("source.png");
        writeImage(imagePath, 6, 4);
        ImageFileInTree imageFile = new ImageFileInTree(imagePath.toFile());

        Image scaled = imageFile.getScaledInstance(new Dimension(12, 8));

        assertNotNull(scaled);
        BufferedImage scaledImage = assertInstanceOf(BufferedImage.class, scaled);
        assertEquals(12, scaledImage.getWidth());
        assertEquals(8, scaledImage.getHeight());
    }

    @Test
    void clampsZeroAndNegativeDimensionsToOne() throws IOException {
        Path imagePath = tempDir.resolve("source.png");
        writeImage(imagePath, 10, 10);
        ImageFileInTree imageFile = new ImageFileInTree(imagePath.toFile());

        Image scaled = imageFile.getScaledInstance(new Dimension(0, -5));

        assertNotNull(scaled);
        BufferedImage scaledImage = assertInstanceOf(BufferedImage.class, scaled);
        assertEquals(1, scaledImage.getWidth());
        assertEquals(1, scaledImage.getHeight());
    }

    @Test
    void returnsNullForUnreadableImageFile() throws IOException {
        Path notAnImage = tempDir.resolve("not-image.txt");
        Files.writeString(notAnImage, "hello");
        ImageFileInTree imageFile = new ImageFileInTree(notAnImage.toFile());

        Image scaled = imageFile.getScaledInstance(new Dimension(10, 10));

        assertNull(scaled);
    }

    private static void writeImage(Path path, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        assertTrue(ImageIO.write(image, "png", path.toFile()));
    }
}
