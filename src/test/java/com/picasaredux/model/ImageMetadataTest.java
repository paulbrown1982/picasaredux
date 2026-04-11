package com.picasaredux.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMetadataTest {

    @TempDir
    Path tempDir;

    @Test
    void generateSummaryMapsCoreFields() throws IOException {
        Path imagePath = tempDir.resolve("sample-image.png");
        Files.write(imagePath, new byte[] {1, 2, 3, 4, 5});
        File imageFile = imagePath.toFile();

        long modifiedMillis = Instant.parse("2026-01-02T03:04:05Z").toEpochMilli();
        assertTrue(imageFile.setLastModified(modifiedMillis));

        BufferedImage image = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_ARGB);
        ImageMetadata.Summary summary = ImageMetadata.generateSummary(image, imageFile);

        assertEquals("sample-image.png", summary.fileName());
        assertEquals(4000, summary.width());
        assertEquals(3000, summary.height());
        assertEquals(4d / 3d, summary.aspectRatio());
        assertEquals(12.0, summary.megapixels());
        assertEquals("png", summary.format());
        assertEquals(5L, summary.fileSizeBytes());
        assertEquals("Landscape", summary.orientation());
        assertEquals(Utils.ukDateFormat(Instant.ofEpochMilli(modifiedMillis)), summary.modified());
        assertEquals("2 January 2026 at 3:04am", summary.modified());
        assertSame(image.getColorModel(), summary.colourModel());
        assertEquals(
                "DirectColorModel: rmask=ff0000 gmask=ff00 bmask=ff amask=ff000000",
                summary.colourModel().toString()
        );
        assertEquals(2, summary.imageType());
        assertEquals("TYPE_INT_ARGB", summary.imageTypeLabel());
    }

    @Test
    void generateSummaryDetectsPortraitAndSquareOrientation() throws IOException {
        Path portraitPath = tempDir.resolve("portrait.jpg");
        Files.write(portraitPath, new byte[] {1});
        BufferedImage portrait = new BufferedImage(1200, 2400, BufferedImage.TYPE_INT_RGB);
        ImageMetadata.Summary portraitSummary = ImageMetadata.generateSummary(portrait, portraitPath.toFile());
        assertEquals("Portrait", portraitSummary.orientation());
        assertEquals("jpg", portraitSummary.format());

        Path squarePath = tempDir.resolve("square");
        Files.write(squarePath, new byte[] {1, 2});
        BufferedImage square = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        ImageMetadata.Summary squareSummary = ImageMetadata.generateSummary(square, squarePath.toFile());
        assertEquals("Square", squareSummary.orientation());
        assertEquals("Unknown", squareSummary.format());
    }
}
