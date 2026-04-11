package com.picasaredux.model;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.icc.IccDirectory;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Optional;

import static com.drew.metadata.exif.ExifDirectoryBase.*;

public class ExifData {

    public record Summary(
            String dateTaken,
            String camera,
            String lens,
            String focalLength,
            String aperture,
            String shutter,
            String iso,
            String gps,
            String orientation,
            String colorSpace,
            String iccProfile,
            String error
    ) {

        private static final DateTimeFormatter EXIF_DATE_FORMAT =
                DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss");

        private static Optional<TemporalAccessor> parseExifDate(String raw) {
            if (raw == null || raw.isBlank() || "0000:00:00 00:00:00".equals(raw)) {
                return Optional.empty();
            }
            try {
                return Optional.of(EXIF_DATE_FORMAT.parse(raw.trim()));
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }

        public String dateTakenUk() {
            return parseExifDate(dateTaken)
                    .map(Utils::ukDateFormat)
                    .orElse("");
        }

        public boolean hasAnyValues() {
            return firstNonBlank(dateTaken, camera, lens, focalLength, aperture, shutter, iso, gps, orientation, colorSpace, iccProfile) != null;
        }
    }

    public static Summary readExifMetadata(File imageFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            IccDirectory iccDirectory = metadata.getFirstDirectoryOfType(IccDirectory.class);

            String make = getString(ifd0, TAG_MAKE);
            String model = getString(ifd0, TAG_MODEL);
            String camera = joinWithSpace(" ", make, model);

            String dateTaken = firstNonBlank(
                    getDescription(subIfd, TAG_DATETIME_ORIGINAL),
                    getDescription(subIfd, TAG_DATETIME_DIGITIZED)
            );

            String gps = null;
            if (gpsDirectory != null) {
                GeoLocation location = gpsDirectory.getGeoLocation();
                if (location != null && !location.isZero()) {
                    gps = String.format(Locale.UK, "%.6f, %.6f", location.getLatitude(), location.getLongitude());
                }
            }

            return new Summary(
                dateTaken,
                camera,
                getDescription(subIfd, TAG_LENS_MODEL),
                getDescription(subIfd, TAG_FOCAL_LENGTH),
                getDescription(subIfd, TAG_FNUMBER),
                getDescription(subIfd, TAG_EXPOSURE_TIME),
                getDescription(subIfd, TAG_ISO_EQUIVALENT),
                gps,
                getDescription(ifd0, TAG_ORIENTATION),
                getDescription(subIfd, TAG_COLOR_SPACE),
                firstNonBlank(
                        getDescription(iccDirectory, IccDirectory.TAG_TAG_desc),
                        getDescription(iccDirectory, IccDirectory.TAG_APPLE_MULTI_LANGUAGE_PROFILE_NAME)
                ),
                null
            );
        } catch (ImageProcessingException | IOException e) {
            return new Summary(null, null, null, null, null, null, null, null, null, null, null, e.getMessage());
        }
    }

    private static String getString(ExifIFD0Directory directory, int tag) {
        return directory == null ? null : Utils.trimToNull(directory.getString(tag));
    }

    private static String getDescription(ExifDirectoryBase directory, int tag) {
        return directory == null ? null : Utils.trimToNull(directory.getDescription(tag));
    }

    private static String getDescription(IccDirectory directory, int tag) {
        return directory == null ? null : Utils.trimToNull(directory.getDescription(tag));
    }

    private static String joinWithSpace(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(value);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
