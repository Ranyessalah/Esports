package controllers.userManagement;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import services.userManagement.FaceIdService;

import java.net.URL;
import java.nio.file.*;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Camera preview popup for Face ID enrollment and authentication.
 * Shows a live webcam feed with face-detection overlay.
 */
public class FaceIdCameraController implements Initializable {

    public enum Mode { ENROLL, AUTHENTICATE }

    @FXML private ImageView cameraView;
    @FXML private Label     statusLabel;
    @FXML private Label     countdownLabel;
    @FXML private Button    actionButton;
    @FXML private Button    cancelButton;

    private VideoCapture             camera;
    private ScheduledExecutorService executor;
    private CascadeClassifier        faceDetector;
    private boolean                  faceDetected = false;

    private Mode             mode;
    private int              userId;
    private Consumer<Boolean> onEnrollResult;       // enrollment callback
    private Consumer<Integer> onAuthResult;          // auth callback (-1 = fail)

    // Enroll state
    private int     savedFrames   = 0;
    private int     totalFrames   = 5;
    private boolean enrolling     = false;
    private boolean enrollDone    = false;

    // ── Initialization ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCascade();
    }

    /** Call this right after FXML load, before showing the stage. */
    public void configure(Mode mode, int userId,
                          Consumer<Boolean> onEnrollResult,
                          Consumer<Integer> onAuthResult) {
        this.mode           = mode;
        this.userId         = userId;
        this.onEnrollResult = onEnrollResult;
        this.onAuthResult   = onAuthResult;

        if (mode == Mode.ENROLL) {
            actionButton.setText("📸  Enregistrer mon visage");
            statusLabel.setText("Placez votre visage dans le cadre");
        } else {
            actionButton.setText("🔓  Identifier");
            statusLabel.setText("Regardez la caméra");
        }

        startCamera();
    }

    // ── Camera ─────────────────────────────────────────────────────────────

    private void startCamera() {
        camera   = new VideoCapture(0);
        executor = Executors.newSingleThreadScheduledExecutor();

        if (!camera.isOpened()) {
            Platform.runLater(() -> statusLabel.setText("❌  Webcam introuvable"));
            return;
        }

        executor.scheduleAtFixedRate(this::grabFrame, 0, 66, TimeUnit.MILLISECONDS);
    }

    private void grabFrame() {
        if (camera == null || !camera.isOpened()) return;

        Mat frame = new Mat();
        camera.read(frame);
        if (frame.empty()) return;

        // Face detection overlay
        Mat display  = frame.clone();
        Rect[] faces = detectFaces(frame);
        faceDetected = faces.length > 0;

        for (Rect r : faces) {
            Imgproc.rectangle(display, r.tl(), r.br(),
                    new Scalar(0, 255, 128), 2);
        }

        // If in auth-auto mode, capture automatically when face found
        if (mode == Mode.AUTHENTICATE && faceDetected && !enrollDone) {
            enrollDone = true;
            performAuthentication(frame);
        }

        // If enrolling, capture frame
        if (enrolling && faceDetected && savedFrames < totalFrames) {
            saveEnrollFrame(frame);
        }

        Image img = matToImage(display);
        Platform.runLater(() -> {
            cameraView.setImage(img);
            if (!enrolling && !enrollDone) {
                statusLabel.setText(faceDetected
                        ? "✅  Visage détecté"
                        : "🔍  Recherche du visage…");
            }
        });
    }

    // ── Enroll ─────────────────────────────────────────────────────────────

    @FXML
    private void onAction() {
        if (mode == Mode.ENROLL) {
            if (!faceDetected) {
                Platform.runLater(() ->
                        statusLabel.setText("⚠  Aucun visage détecté. Repositionnez-vous."));
                return;
            }
            savedFrames = 0;
            enrollDone  = false;
            enrolling   = true;
            actionButton.setDisable(true);
            statusLabel.setText("📸  Capture en cours…");

        } else {
            // Manual auth trigger (backup)
            if (!faceDetected) {
                statusLabel.setText("⚠  Aucun visage détecté.");
                return;
            }
            enrollDone = true;
            Mat frame = new Mat();
            camera.read(frame);
            performAuthentication(frame);
        }
    }

    private void saveEnrollFrame(Mat frame) {
        try {
            Path dir = Paths.get("face_data", String.valueOf(userId));
            Files.createDirectories(dir);

            // Convert to grayscale + resize
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);

            Rect[] faces = detectFaces(frame);
            if (faces.length == 0) return;

            Rect best = faces[0];
            for (Rect r : faces) if (r.area() > best.area()) best = r;

            Mat crop    = new Mat(gray, best);
            Mat resized = new Mat();
            Imgproc.resize(crop, resized, new Size(100, 100));

            String path = dir.resolve("face_" + savedFrames + ".png").toString();
            org.opencv.imgcodecs.Imgcodecs.imwrite(path, resized);

            savedFrames++;
            int remaining = totalFrames - savedFrames;

            Platform.runLater(() -> {
                countdownLabel.setText(remaining > 0 ? String.valueOf(remaining) : "");
                statusLabel.setText("📸  " + savedFrames + "/" + totalFrames + " captures");
            });

            if (savedFrames >= totalFrames) {
                enrolling  = true;
                enrollDone = true;
                enrolling  = false;
                stopCamera();
                Platform.runLater(() -> {
                    statusLabel.setText("✅  Visage enregistré avec succès !");
                    countdownLabel.setText("");
                    if (onEnrollResult != null) onEnrollResult.accept(true);
                    closeStage();
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Authenticate ───────────────────────────────────────────────────────

    private void performAuthentication(Mat frame) {
        Platform.runLater(() -> {
            statusLabel.setText("🔍  Vérification…");
            actionButton.setDisable(true);
        });

        new Thread(() -> {
            try {
                int result = FaceIdService.getInstance()
                        .authenticate().get();

                stopCamera();
                Platform.runLater(() -> {
                    if (result >= 0) {
                        statusLabel.setText("✅  Identifié !");
                    } else {
                        statusLabel.setText("❌  Visage non reconnu");
                    }
                    if (onAuthResult != null) onAuthResult.accept(result);
                    closeStage();
                });
            } catch (Exception e) {
                stopCamera();
                Platform.runLater(() -> {
                    statusLabel.setText("❌  Erreur : " + e.getMessage());
                    if (onAuthResult != null) onAuthResult.accept(-1);
                    closeStage();
                });
            }
        }).start();
    }

    // ── Cancel ─────────────────────────────────────────────────────────────

    @FXML
    private void onCancel() {
        stopCamera();
        if (mode == Mode.ENROLL && onEnrollResult != null) onEnrollResult.accept(false);
        if (mode == Mode.AUTHENTICATE && onAuthResult != null) onAuthResult.accept(-1);
        closeStage();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void loadCascade() {
        try {
            nu.pattern.OpenCV.loadLocally();
            // Reuse the same logic from FaceIdService
            faceDetector = FaceIdService.getInstance().getCascadeClassifier();
            if (faceDetector != null && !faceDetector.empty()) {
                System.out.println("[FaceIdCamera] Cascade ready ✓");
            }
        } catch (Exception e) {
            System.err.println("[FaceIdCamera] Cascade load failed: " + e.getMessage());
        }
    }
    private Rect[] detectFaces(Mat frame) {
        if (faceDetector == null || faceDetector.empty()) return new Rect[0];
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 4, 0,
                new Size(80, 80), new Size(400, 400));
        return faces.toArray();
    }

    private Image matToImage(Mat mat) {
        MatOfByte mob = new MatOfByte();
        org.opencv.imgcodecs.Imgcodecs.imencode(".png", mat, mob);
        byte[] bytes = mob.toArray();
        return new Image(new java.io.ByteArrayInputStream(bytes));
    }

    private void stopCamera() {
        if (executor != null) { executor.shutdownNow(); executor = null; }
        if (camera   != null) { camera.release();       camera   = null; }
    }

    private void closeStage() {
        Platform.runLater(() -> {
            Stage s = (Stage) cancelButton.getScene().getWindow();
            if (s != null) s.close();
        });
    }
}