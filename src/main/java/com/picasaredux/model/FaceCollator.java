package com.picasaredux.model;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_img_hash.ImgHashBase;
import org.bytedeco.opencv.opencv_img_hash.PHash;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

public final class FaceCollator {

    private static final int FACE_DISTANCE_THRESHOLD = 12;

    private final OpenCvFaceDetector detector = new OpenCvFaceDetector();
    private final ImgHashBase hashAlgorithm = PHash.create();

    public List<List<ImageFileInTree>> cluster(List<ImageFileInTree> images) {
        List<Cluster> clusters = new ArrayList<>();
        try {
            for (ImageFileInTree image : images) {
                List<Mat> signatures = faceSignaturesForImage(image);
                try {
                    if (signatures.isEmpty()) {
                        continue;
                    }

                    Cluster bestCluster = null;
                    Mat bestSignature = null;
                    double bestDistance = Double.POSITIVE_INFINITY;
                    for (Mat signature : signatures) {
                        for (Cluster cluster : clusters) {
                            double distance = hashAlgorithm.compare(cluster.center(), signature);
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
                    signatures.forEach(Mat::release);
                }
            }

            return clusters.stream()
                    .sorted(Comparator.comparingInt(Cluster::imageCount).reversed())
                    .map(Cluster::images)
                    .toList();
        } finally {
            clusters.forEach(Cluster::release);
        }
    }

    private List<Mat> faceSignaturesForImage(ImageFileInTree image) {
        List<Rectangle> faces = detector.detectFaces(image.getUnderlying());
        if (faces.isEmpty()) {
            return List.of();
        }

        Mat source = imread(image.getUnderlying().getAbsolutePath(), IMREAD_GRAYSCALE);
        if (source == null) {
            return List.of();
        }

        try {
            if (source.empty()) {
                return List.of();
            }
            List<Mat> signatures = new ArrayList<>(faces.size());
            for (Rectangle face : faces) {
                Rectangle bounds = face.intersection(new Rectangle(0, 0, source.cols(), source.rows()));
                if (bounds.width < 8 || bounds.height < 8) {
                    continue;
                }
                Mat crop = new Mat(source, new org.bytedeco.opencv.opencv_core.Rect(
                        bounds.x, bounds.y, bounds.width, bounds.height));
                try {
                    signatures.add(signatureFor(crop));
                } finally {
                    crop.release();
                }
            }
            return signatures;
        } finally {
            source.release();
        }
    }

    private Mat signatureFor(Mat crop) {
        Mat hash = new Mat();
        hashAlgorithm.compute(crop, hash);
        return hash;
    }

    private static final class Cluster {
        private final Set<ImageFileInTree> images = new LinkedHashSet<>();
        private Mat center;

        private Cluster() {
        }

        private Mat center() {
            return center;
        }

        private void add(ImageFileInTree image, Mat signature) {
            images.add(image);
            if (center != null) {
                center.release();
            }
            center = signature.clone();
        }

        private List<ImageFileInTree> images() {
            return List.copyOf(images);
        }

        private int imageCount() {
            return images.size();
        }

        private void release() {
            if (center != null) {
                center.release();
            }
        }
    }
}
