package com.picasaredux.model;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FaceCollator {

    private static final int SIGNATURE_SIZE = 16;
    private static final int FACE_DISTANCE_THRESHOLD = 12;

    private final OpenCvFaceDetector detector = new OpenCvFaceDetector();

    public List<List<ImageFileInTree>> cluster(List<ImageFileInTree> images) {
        List<Cluster> clusters = new ArrayList<>();
        for (ImageFileInTree image : images) {
            for (long signature : faceSignaturesForImage(image)) {
                Cluster best = findClosestCluster(clusters, signature);
                if (best == null) {
                    Cluster created = new Cluster(signature);
                    created.add(image, signature);
                    clusters.add(created);
                } else {
                    best.add(image, signature);
                }
            }
        }

        return clusters.stream()
                .sorted(Comparator.comparingInt(Cluster::imageCount).reversed())
                .map(Cluster::images)
                .toList();
    }

    private List<Long> faceSignaturesForImage(ImageFileInTree image) {
        List<Rectangle> faces = detector.detectFaces(image.getUnderlying());
        if (faces.isEmpty()) {
            return List.of();
        }

        BufferedImage source = loadImage(image);
        if (source == null) {
            return List.of();
        }

        try {
            List<Long> signatures = new ArrayList<>(faces.size());
            for (Rectangle face : faces) {
                Rectangle bounds = face.intersection(new Rectangle(0, 0, source.getWidth(), source.getHeight()));
                if (bounds.width < 8 || bounds.height < 8) {
                    continue;
                }
                BufferedImage crop = source.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
                signatures.add(signatureFor(crop));
            }
            return signatures;
        } finally {
            source.flush();
        }
    }

    private static BufferedImage loadImage(ImageFileInTree image) {
        try {
            return ImageIO.read(image.getUnderlying());
        } catch (IOException ignored) {
            return null;
        }
    }

    private static long signatureFor(BufferedImage crop) {
        BufferedImage scaled = new BufferedImage(SIGNATURE_SIZE, SIGNATURE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(crop, 0, 0, SIGNATURE_SIZE, SIGNATURE_SIZE, null);
        g.dispose();

        int[] values = new int[SIGNATURE_SIZE * SIGNATURE_SIZE];
        int total = 0;
        int idx = 0;
        for (int y = 0; y < SIGNATURE_SIZE; y++) {
            for (int x = 0; x < SIGNATURE_SIZE; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int gVal = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int grayscale = (r + gVal + b) / 3;
                values[idx++] = grayscale;
                total += grayscale;
            }
        }

        double mean = total / (double) values.length;
        long top = 0L;
        long bottom = 0L;
        for (int i = 0; i < values.length; i++) {
            if (values[i] >= mean) {
                if (i < 64) {
                    top |= (1L << i);
                } else {
                    bottom |= (1L << (i - 64));
                }
            }
        }
        return top ^ Long.rotateLeft(bottom, 1);
    }

    private static Cluster findClosestCluster(List<Cluster> clusters, long signature) {
        Cluster best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Cluster cluster : clusters) {
            int distance = hammingDistance(cluster.center(), signature);
            if (distance <= FACE_DISTANCE_THRESHOLD && distance < bestDistance) {
                best = cluster;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    private static final class Cluster {
        private final Set<ImageFileInTree> images = new LinkedHashSet<>();
        private long center;

        private Cluster(long center) {
            this.center = center;
        }

        private long center() {
            return center;
        }

        private void add(ImageFileInTree image, long signature) {
            images.add(image);
            center = center ^ ((center ^ signature) >>> 1);
        }

        private List<ImageFileInTree> images() {
            return List.copyOf(images);
        }

        private int imageCount() {
            return images.size();
        }
    }
}
