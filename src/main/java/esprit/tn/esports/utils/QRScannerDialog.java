package esprit.tn.esports.utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Utility Dialog to scan QR codes using the computer webcam.
 */
public class QRScannerDialog {

    private Webcam webcam = null;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    public void startScanner(Consumer<String> onScanSuccess) {
        Stage stage = new Stage();
        stage.setTitle("Scanner de Code QR");
        stage.initModality(Modality.APPLICATION_MODAL);

        // 1. Initialize Webcam
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    a.setTitle("Webcam");
                    a.setHeaderText(null);
                    a.setContentText("Aucune webcam détectée. Branchez une caméra ou autorisez l’accès, puis réessayez.");
                    a.showAndWait();
                });
                return;
            }
            webcam.setViewSize(WebcamResolution.VGA.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setHeaderText(null);
                a.setContentText("Impossible d’initialiser la webcam : " + e.getMessage());
                a.showAndWait();
            });
            return;
        }

        // 2. Create Swing Panel for Webcam View
        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSDisplayed(true);
        panel.setFillArea(true);

        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> swingNode.setContent(panel));

        // 3. Layout
        StackPane root = new StackPane(swingNode);
        stage.setScene(new Scene(root, 640, 480));

        // 4. Background Scanning Logic
        isScanning.set(true);
        Thread thread = new Thread(() -> {
            while (isScanning.get()) {
                try {
                    Thread.sleep(100); // Poll every 100ms

                    if (!webcam.isOpen()) continue;

                    BufferedImage image = webcam.getImage();
                    if (image == null) continue;

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try {
                        Result result = new MultiFormatReader().decode(bitmap);
                        if (result != null) {
                            String code = result.getText();
                            isScanning.set(false);

                            // Success! Return to JavaFX Thread
                            Platform.runLater(() -> {
                                stopCamera();
                                stage.close();
                                onScanSuccess.accept(code);
                            });
                        }
                    } catch (NotFoundException e) {
                        // QR Code not found in this frame, keep trying
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        stage.setOnCloseRequest(event -> stopCamera());
        stage.show();
    }

    private void stopCamera() {
        isScanning.set(false);
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
