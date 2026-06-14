package com.picasaredux.service;

import com.picasaredux.model.ImageFileInTree;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_img_hash.ImgHashBase;
import org.bytedeco.opencv.opencv_img_hash.ColorMomentHash;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

public final class FaceCollator {

    // Tight enough to keep visually distinct faces apart while still grouping near-identical crops.
    private static final double FACE_DISTANCE_THRESHOLD = 0.5;
    private static final double FACE_CROP_PADDING_RATIO = 0.15;

    private final FaceDetector detector = new FaceDetector();
    private final ImgHashBase hashAlgorithm = ColorMomentHash.create();

    public List<List<ImageFileInTree>> cluster(List<ImageFileInTree> images) {
        List<Cluster> clusters = new ArrayList<>();
        try {
            for (ImageFileInTree image : images) {
                List<Signature> signatures = getFaceSignaturesForImage(image);
                try {
                    if (signatures.isEmpty()) {
                        continue;
                    }

                    Cluster bestCluster = null;
                    Signature bestSignature = null;
                    double bestDistance = Double.POSITIVE_INFINITY;
                    for (Signature signature : signatures) {
                        for (Cluster cluster : clusters) {
                            double distance = cluster.bestDistance(signature, hashAlgorithm);
                            if (distance <= FACE_DISTANCE_THRESHOLD && distance < bestDistance) {
                                bestCluster = cluster;
                                bestSignature = signature;
                                bestDistance = distance;
                            }
                        }
                    }

                    if (bestCluster == null) {
                        Cluster created = new Cluster();
                        created.add(image, signatures.getFirst());
                        clusters.add(created);
                    } else {
                        bestCluster.add(image, bestSignature);
                    }
                } finally {
                    signatures.forEach(Signature::release);
                }
            }

            return clusters.stream()
                    .sorted(Comparator.comparingInt(Cluster::imageCount).reversed())
                    .map(Cluster::getImages)
                    .toList();
        } finally {
            clusters.forEach(Cluster::release);
        }
    }

    private List<Signature> getFaceSignaturesForImage(ImageFileInTree image) {
        List<Rectangle> faces = detector.detectFaces(image.getUnderlying());
        if (faces.isEmpty()) {
            return List.of();
        }

        Rectangle face = faces.stream()
                .max(Comparator.comparingInt(r -> r.width * r.height))
                .orElseThrow();

        Mat source = imread(image.getUnderlying().getAbsolutePath(), IMREAD_GRAYSCALE);
        if (source == null) {
            return List.of();
        }

        try {
            if (source.empty()) {
                return List.of();
            }
            Rectangle bounds = paddedBounds(face, source.cols(), source.rows());
            if (bounds.width < 8 || bounds.height < 8) {
                return List.of();
            }
            Mat crop = new Mat(source, new Rect(bounds.x, bounds.y, bounds.width, bounds.height));
            try {
                return List.of(signatureFor(crop));
            } finally {
                crop.release();
            }
        } finally {
            source.release();
        }
    }

    private static Rectangle paddedBounds(Rectangle face, int width, int height) {
        int padX = (int) Math.round(face.width * FACE_CROP_PADDING_RATIO);
        int padY = (int) Math.round(face.height * FACE_CROP_PADDING_RATIO);
        Rectangle expanded = new Rectangle(
                face.x - padX,
                face.y - padY,
                face.width + (padX * 2),
                face.height + (padY * 2));
        return expanded.intersection(new Rectangle(0, 0, width, height));
    }

    private Signature signatureFor(Mat crop) {
        Mat hash = new Mat();
        hashAlgorithm.compute(crop, hash);
        return new Signature(hash);
    }

    private static final class Cluster {
        private final Set<ImageFileInTree> images = new LinkedHashSet<>();
        private final List<Signature> signatures = new ArrayList<>();

        private Cluster() {
        }

        private void add(ImageFileInTree image, Signature signature) {
            if (images.add(image)) {
                signatures.add(signature.copy());
            }
        }

        private double bestDistance(Signature signature, ImgHashBase hashAlgorithm) {
            double best = Double.POSITIVE_INFINITY;
            for (Signature clusterSignature : signatures) {
                double distance = hashAlgorithm.compare(clusterSignature.underlying(), signature.underlying());
                if (distance < best) {
                    best = distance;
                }
            }
            return best;
        }

        private List<ImageFileInTree> getImages() {
            return List.copyOf(images);
        }

        private int imageCount() {
            return images.size();
        }

        private void release() {
            signatures.forEach(Signature::release);
        }
    }

    private record Signature(Mat underlying) {

        private Signature copy() {
            return new Signature(underlying.clone());
        }

        void release() {
            underlying.release();
        }
    }
}
