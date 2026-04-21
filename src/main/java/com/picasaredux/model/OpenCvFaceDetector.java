package com.picasaredux.model;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

final class OpenCvFaceDetector {

    private static final String CASCADE_FILE = "haarcascade_frontalface_alt.xml";
    private static final Path CASCADE_PATH = loadCascadePath();

    private final ThreadLocal<CascadeClassifier> detector = ThreadLocal.withInitial(OpenCvFaceDetector::newClassifier);

    public OpenCvFaceDetector() {
    }

    public boolean hasFace(java.io.File file) {
        Mat grayscale = imread(file.getAbsolutePath(), IMREAD_GRAYSCALE);
        if (grayscale == null || grayscale.empty()) {
            return false;
        }

        RectVector faces = new RectVector();
        detector.get().detectMultiScale(grayscale, faces);
        return faces.size() > 0;
    }

    private static CascadeClassifier newClassifier() {
        CascadeClassifier classifier = new CascadeClassifier(CASCADE_PATH.toString());
        if (classifier.empty()) {
            throw new IllegalStateException("Could not load OpenCV face cascade classifier");
        }
        return classifier;
    }

    private static Path loadCascadePath() {
        URL cascadeResource = resolveCascadeResource();
        if (cascadeResource == null) {
            throw new IllegalStateException("Could not find OpenCV face cascade resource: " + CASCADE_FILE);
        }

        try {
            return Loader.cacheResource(cascadeResource).toPath();
        } catch (IOException e) {
            throw new IllegalStateException("Could not cache OpenCV face cascade resource", e);
        }
    }

    private static URL resolveCascadeResource() {
        String platform = Loader.getPlatform();
        List<String> candidates = List.of(
                "/org/bytedeco/opencv/" + platform + "/share/opencv4/haarcascades/" + CASCADE_FILE,
                "/org/bytedeco/opencv/" + platform + "/share/opencv/haarcascades/" + CASCADE_FILE,
                "/org/bytedeco/opencv/" + platform + "/share/OpenCV/haarcascades/" + CASCADE_FILE
        );

        for (String candidate : candidates) {
            URL resource = OpenCvFaceDetector.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }
}
