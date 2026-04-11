package com.picasaredux.model;

import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.icc.IccDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.drew.metadata.exif.ExifDirectoryBase.*;
import static org.junit.jupiter.api.Assertions.*;

class ExifDataTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsErrorWhenFileCannotBeRead() {
        File missing = tempDir.resolve("missing.jpg").toFile();

        ExifData.Summary summary = ExifData.readExifMetadata(missing);

        assertNotNull(summary.error());
        assertFalse(summary.hasAnyValues());
    }

    @Test
    void returnsEmptySummaryForImageWithoutExif() throws IOException {
        Path imagePath = tempDir.resolve("plain.png");
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        assertTrue(ImageIO.write(image, "png", imagePath.toFile()));

        ExifData.Summary summary = ExifData.readExifMetadata(imagePath.toFile());

        assertNull(summary.error());
        assertFalse(summary.hasAnyValues());
        assertEquals("", summary.dateTakenUk());
    }

    @Test
    void mapsExifFieldsFromMetadata() {
        Metadata metadata = new Metadata();

        ExifIFD0Directory ifd0 = new ExifIFD0Directory();
        ifd0.setString(TAG_MAKE, " Canon ");
        ifd0.setString(TAG_MODEL, " EOS R5 ");
        ifd0.setInt(TAG_ORIENTATION, 1);
        metadata.addDirectory(ifd0);

        ExifSubIFDDirectory subIfd = new ExifSubIFDDirectory();
        subIfd.setString(TAG_DATETIME_ORIGINAL, "2025:04:10 09:08:07");
        subIfd.setString(TAG_LENS_MODEL, " RF24-70mm F2.8 L IS USM ");
        subIfd.setRational(TAG_FOCAL_LENGTH, new Rational(50, 1));
        subIfd.setRational(TAG_FNUMBER, new Rational(28, 10));
        subIfd.setRational(TAG_EXPOSURE_TIME, new Rational(1, 125));
        subIfd.setInt(TAG_ISO_EQUIVALENT, 100);
        subIfd.setInt(TAG_COLOR_SPACE, 1);
        metadata.addDirectory(subIfd);

        GpsDirectory gpsDirectory = new GpsDirectory();
        gpsDirectory.setRationalArray(GpsDirectory.TAG_LATITUDE, new Rational[] {
                new Rational(51, 1), new Rational(30, 1), new Rational(0, 1)
        });
        gpsDirectory.setString(GpsDirectory.TAG_LATITUDE_REF, "N");
        gpsDirectory.setRationalArray(GpsDirectory.TAG_LONGITUDE, new Rational[] {
                new Rational(0, 1), new Rational(7, 1), new Rational(30, 1)
        });
        gpsDirectory.setString(GpsDirectory.TAG_LONGITUDE_REF, "W");
        metadata.addDirectory(gpsDirectory);

        IccDirectory iccDirectory = new IccDirectory();
        iccDirectory.setString(IccDirectory.TAG_TAG_desc, "Display P3");
        metadata.addDirectory(iccDirectory);

        ExifData.Summary summary = ExifData.readExifMetadata(metadata);

        assertNull(summary.error());
        assertEquals("2025:04:10 09:08:07", summary.dateTaken());
        assertEquals("Canon EOS R5", summary.camera());
        assertEquals("RF24-70mm F2.8 L IS USM", summary.lens());
        assertEquals("50 mm", summary.focalLength());
        assertTrue(summary.aperture().contains("2.8"));
        assertTrue(summary.shutter().contains("1/125"));
        assertEquals("100", summary.iso());
        assertEquals("51.500000, -0.125000", summary.gps());
        assertNotNull(summary.orientation());
        assertEquals("sRGB", summary.colorSpace());
        assertNotNull(summary.iccProfile());
        assertTrue(summary.iccProfile().contains("Disp"));
        assertTrue(summary.hasAnyValues());
    }
}
