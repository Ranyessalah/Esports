package services;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Face ID service using OpenCV Haar cascade for detection
 * and histogram-based comparison for recognition.
 *
 * Dependencies (add to pom.xml):
 *   <dependency>
 *     <groupId>org.openpnp</groupId>
 *     <artifactId>opencv</artifactId>
 *     <version>4.7.0-0</version>
 *   </dependency>
 */
public class FaceIdService {

    // ── Constants ──────────────────────────────────────────────────────────
    private static final String FACE_DATA_DIR   = "face_data";
    private static final int    CAPTURE_FRAMES  = 5;       // frames to average
    private static final double MATCH_THRESHOLD = 0.65;    // histogram correlation
    private static final int    FACE_SIZE       = 100;     // normalized face px

    private CascadeClassifier faceDetector;
    private boolean           opencvLoaded = false;

    // ── Singleton ──────────────────────────────────────────────────────────
    private static FaceIdService instance;
    public static FaceIdService getInstance() {
        if (instance == null) instance = new FaceIdService();
        return instance;
    }

    private FaceIdService() {
        loadOpenCV();
    }

    // ── OpenCV Bootstrap ───────────────────────────────────────────────────

    private void loadOpenCV() {
        try {
            nu.pattern.OpenCV.loadLocally();
            faceDetector = loadCascadeFromJar();
            if (faceDetector == null || faceDetector.empty()) {
                throw new Exception("CascadeClassifier is empty after loading.");
            }
            opencvLoaded = true;
            System.out.println("[FaceId] OpenCV + cascade loaded ✓");
        } catch (Exception e) {
            System.err.println("[FaceId] OpenCV load failed: " + e.getMessage());
            opencvLoaded = false;
        }
    }

    /**
     * Extracts haarcascade_frontalface_default.xml from wherever it can be found:
     *  1. Your own src/main/resources  (classpath root)
     *  2. Inside the openpnp opencv jar  (nu/pattern/opencv/...)
     *  3. Python/system OpenCV data dir  (last resort, environment-specific)
     */
    private CascadeClassifier loadCascadeFromJar() throws Exception {
        InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
        if (is == null) {
            throw new Exception("haarcascade_frontalface_default.xml not found in resources.");
        }
        Path tmp = Files.createTempFile("haarcascade_frontalface", ".xml");
        tmp.toFile().deleteOnExit();
        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
        is.close();

        CascadeClassifier cc = new CascadeClassifier(tmp.toString());
        if (cc.empty()) throw new Exception("CascadeClassifier loaded but is empty.");
        return cc;
    }
    /**
     * Scans every JAR on the current classloader's classpath for the cascade XML.
     * Works even if the file is at an unexpected internal path in the openpnp jar.



     /** Expose the loaded classifier so FaceIdCameraController can reuse it. */
    public CascadeClassifier getCascadeClassifier() {
        return faceDetector;
    }
    private InputStream findCascadeInClasspath() {
        try {
            ClassLoader cl = getClass().getClassLoader();
            if (cl instanceof java.net.URLClassLoader ucl) {
                for (java.net.URL jarUrl : ucl.getURLs()) {
                    try {
                        java.net.URLConnection conn = jarUrl.openConnection();
                        if (conn instanceof java.net.JarURLConnection jc) {
                            java.util.jar.JarFile jar = jc.getJarFile();
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                String name = entries.nextElement().getName();
                                if (name.endsWith("haarcascade_frontalface_default.xml")) {
                                    System.out.println("[FaceId] Found in jar: " + name);
                                    return cl.getResourceAsStream(name);
                                }
                            }
                        } else if (jarUrl.getProtocol().equals("file")) {
                            // It's a directory or plain jar — try direct resource lookup
                            File f = new File(jarUrl.toURI());
                            if (f.isFile() && f.getName().endsWith(".jar")) {
                                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(f)) {
                                    java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                                    while (entries.hasMoreElements()) {
                                        String name = entries.nextElement().getName();
                                        if (name.endsWith("haarcascade_frontalface_default.xml")) {
                                            System.out.println("[FaceId] Found in jar file: " + f + " → " + name);
                                            return cl.getResourceAsStream(name);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[FaceId] Classpath scan failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Tries to locate the cascade via the python opencv-python package path,
     * which is present on many developer machines.
     */
    private String findPythonOpenCVCascade() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", "-c",
                    "import cv2, os; print(os.path.join(os.path.dirname(cv2.__file__)," +
                            " 'data', 'haarcascade_frontalface_default.xml'))"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return output.isEmpty() ? null : output;
        } catch (Exception e) {
            return null;
        }
    }
    private void initCascade() throws Exception {
        // Extract cascade XML from classpath to a temp file so OpenCV can read it
        InputStream in = getClass().getResourceAsStream(
                "/haarcascade_frontalface_default.xml");
        if (in == null) {
            // Fall back to OpenCV's bundled path
            faceDetector = new CascadeClassifier();
            if (!faceDetector.load("haarcascade_frontalface_default.xml")) {
                throw new Exception("Cannot load Haar cascade.");
            }
            return;
        }
        Path tmp = Files.createTempFile("haarcascade", ".xml");
        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        faceDetector = new CascadeClassifier(tmp.toString());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public boolean isAvailable() { return opencvLoaded; }

    /**
     * Enroll a face for the given userId.
     * Captures CAPTURE_FRAMES frames, extracts the face region, and saves them.
     *
     * @return CompletableFuture<Boolean> — true on success
     */
    public CompletableFuture<Boolean> enrollFace(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!opencvLoaded) return false;

            VideoCapture cap = new VideoCapture(0);
            if (!cap.isOpened()) throw new RuntimeException("Impossible d'ouvrir la webcam.");

            try {
                Path dir = getUserFaceDir(userId);
                Files.createDirectories(dir);

                // Clear any old data
                try (var ds = Files.list(dir)) {
                    ds.forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
                }

                int saved = 0;
                int attempts = 0;

                while (saved < CAPTURE_FRAMES && attempts < 60) {
                    Mat frame = new Mat();
                    cap.read(frame);
                    if (frame.empty()) { attempts++; continue; }

                    Mat face = extractFace(frame);
                    if (face != null) {
                        String path = dir.resolve("face_" + saved + ".png").toString();
                        Imgcodecs.imwrite(path, face);
                        saved++;
                    }
                    attempts++;
                    Thread.sleep(100);
                }
                return saved == CAPTURE_FRAMES;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                cap.release();
            }
        });
    }

    /**
     * Authenticate via face.
     * Captures a live frame and compares against all enrolled users.
     *
     * @return userId if matched, -1 otherwise
     */
    public CompletableFuture<Integer> authenticate() {
        return CompletableFuture.supplyAsync(() -> {
            if (!opencvLoaded) return -1;

            VideoCapture cap = new VideoCapture(0);
            if (!cap.isOpened()) throw new RuntimeException("Impossible d'ouvrir la webcam.");

            try {
                Mat liveFace = null;
                int attempts = 0;

                // Try up to 3 seconds to get a clear face
                while (liveFace == null && attempts < 30) {
                    Mat frame = new Mat();
                    cap.read(frame);
                    if (!frame.empty()) liveFace = extractFace(frame);
                    attempts++;
                    Thread.sleep(100);
                }

                if (liveFace == null) return -1;

                return findBestMatch(liveFace);

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                cap.release();
            }
        });
    }

    /**
     * Check whether a specific user has face data enrolled.
     */
    public boolean hasFaceData(int userId) {
        try {
            Path dir = getUserFaceDir(userId);
            if (!Files.exists(dir)) return false;
            try (var ds = Files.list(dir)) {
                return ds.anyMatch(p -> p.toString().endsWith(".png"));
            }
        } catch (Exception e) { return false; }
    }

    /**
     * Delete face data for a user (e.g. to re-enroll).
     */
    public void deleteFaceData(int userId) {
        try {
            Path dir = getUserFaceDir(userId);
            if (Files.exists(dir)) {
                try (var ds = Files.list(dir)) {
                    ds.forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
                }
                Files.deleteIfExists(dir);
            }
        } catch (Exception ignored) {}
    }

    // ── Core Matching ──────────────────────────────────────────────────────

    /**
     * Compare live face against every enrolled user and return the best-matching userId.
     */
    private int findBestMatch(Mat liveFace) throws Exception {
        Path root = Paths.get(FACE_DATA_DIR);
        if (!Files.exists(root)) return -1;

        Mat liveHist = computeHistogram(liveFace);

        double bestScore  = -1;
        int    bestUserId = -1;

        try (var userDirs = Files.list(root)) {
            for (Path userDir : (Iterable<Path>) userDirs::iterator) {
                if (!Files.isDirectory(userDir)) continue;

                int userId;
                try { userId = Integer.parseInt(userDir.getFileName().toString()); }
                catch (NumberFormatException e) { continue; }

                double score = computeUserScore(liveHist, userDir);
                System.out.printf("[FaceId] user %d → score %.4f%n", userId, score);

                if (score > bestScore) {
                    bestScore  = score;
                    bestUserId = userId;
                }
            }
        }

        return (bestScore >= MATCH_THRESHOLD) ? bestUserId : -1;
    }

    /** Average histogram correlation across all stored face images for a user. */
    private double computeUserScore(Mat liveHist, Path userDir) throws Exception {
        List<Double> scores = new ArrayList<>();
        try (var files = Files.list(userDir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                if (!p.toString().endsWith(".png")) continue;
                Mat stored = Imgcodecs.imread(p.toString());
                if (stored.empty()) continue;
                Mat storedHist = computeHistogram(stored);
                double s = Imgproc.compareHist(liveHist, storedHist, Imgproc.CV_COMP_CORREL);
                scores.add(s);
            }
        }
        if (scores.isEmpty()) return -1;
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(-1);
    }

    // ── Face Extraction ────────────────────────────────────────────────────

    /**
     * Detect the largest face in a frame and return a normalized grayscale crop.
     * Returns null if no face is detected.
     */
    private Mat extractFace(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(
                gray, faces,
                1.1, 4,
                0,
                new Size(80, 80),
                new Size(400, 400)
        );

        Rect[] rects = faces.toArray();
        if (rects.length == 0) return null;

        // Pick the largest detected face
        Rect best = rects[0];
        for (Rect r : rects) if (r.area() > best.area()) best = r;

        Mat face = new Mat(gray, best);
        Mat resized = new Mat();
        Imgproc.resize(face, resized, new Size(FACE_SIZE, FACE_SIZE));
        return resized;
    }

    // ── Histogram ─────────────────────────────────────────────────────────

    private Mat computeHistogram(Mat face) {
        Mat hist = new Mat();
        List<Mat> images = List.of(face);
        MatOfInt  channels = new MatOfInt(0);
        MatOfInt  histSize = new MatOfInt(256);
        MatOfFloat ranges  = new MatOfFloat(0f, 256f);
        Imgproc.calcHist(images, channels, new Mat(), hist, histSize, ranges);
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Path getUserFaceDir(int userId) {
        return Paths.get(FACE_DATA_DIR, String.valueOf(userId));
    }
}