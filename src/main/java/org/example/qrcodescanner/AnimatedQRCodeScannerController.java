package org.example.qrcodescanner;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.animation.AnimationTimer;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.core.Mat;
import javafx.scene.image.Image;
import com.sparrowwallet.hummingbird.URDecoder;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;

public class AnimatedQRCodeScannerController {
    @FXML
    private ImageView imageView;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label statusLabel;
    @FXML
    private TextArea resultLabel;
    @FXML
    private Button copyButton;
    @FXML
    private ComboBox<String> cameraSelector;

    private VideoCapture capture;
    private AnimationTimer timer;
    private boolean isScanning = false;
    private URDecoder urDecoder;
    private ObservableList<String> availableCameras;

    @FXML
    public void initialize() {
        nu.pattern.OpenCV.loadShared();
        capture = new VideoCapture();
        urDecoder = new URDecoder();
        copyButton.setDisable(true);
        
        // Initialize and find cameras
        availableCameras = FXCollections.observableArrayList();
        findAvailableCameras();
        cameraSelector.setItems(availableCameras);
        
        // Try to find the best available camera
        selectDefaultCamera();
    }

    private void findAvailableCameras() {
        availableCameras.clear();
        for (int i = 0; i < 5; i++) {
            VideoCapture temp = new VideoCapture();
            if (temp.open(i)) {
                availableCameras.add("Camera " + i);
                temp.release();
            }
        }
    }

    private void selectDefaultCamera() {
        if (!availableCameras.isEmpty()) {
            // Try to find built-in camera first (usually index 0)
            if (availableCameras.contains("Camera 0")) {
                cameraSelector.getSelectionModel().select("Camera 0");
            } else {
                // If no built-in camera, select the first available one
                cameraSelector.getSelectionModel().selectFirst();
            }
            
            // Update status to show which camera was selected
            String selectedCamera = cameraSelector.getSelectionModel().getSelectedItem();
            statusLabel.setText("Selected " + selectedCamera);
        } else {
            statusLabel.setText("No cameras found");
            startButton.setDisable(true);
        }
    }

    @FXML
    protected void onStartButtonClick() {
        try {
            statusLabel.setText("Initializing camera...");
            resultLabel.clear();
            copyButton.setDisable(true);
            
            if (!capture.isOpened()) {
                int selectedIndex = cameraSelector.getSelectionModel().getSelectedIndex();
                if (selectedIndex == -1) {
                    statusLabel.setText("Please select a camera");
                    return;
                }
                
                boolean opened = capture.open(selectedIndex);
                if (!opened) {
                    statusLabel.setText("Failed to open camera");
                    return;
                }
                
                capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
                capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);

                timer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        Mat frame = new Mat();
                        if (capture.read(frame)) {
                            Image image = mat2Image(frame);
                            imageView.setImage(image);
                            
                            try {
                                BufferedImage bufferedImage = matToBufferedImage(frame);
                                String result = decodeQRCode(bufferedImage);
                                
                                if (result != null) {
                                    statusLabel.setText("Scanning... Found part");
                                    
                                    urDecoder.receivePart(result);
                                    if (urDecoder.getResult() != null) {
                                        String finalResult = urDecoder.getResult().ur.toString();
                                        resultLabel.setText(finalResult);
                                        copyButton.setDisable(false);
                                        statusLabel.setText("Scan Complete!");
                                        
                                        onStopButtonClick();
                                    } else {
                                        double progress = urDecoder.getEstimatedPercentComplete();
                                        statusLabel.setText(String.format("Processing... %.0f%%", progress * 100));
                                    }
                                }
                            } catch (NotFoundException e) {
                                // Silently ignore when no QR code is found
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        frame.release();
                    }
                };
                timer.start();
                
                startButton.setDisable(true);
                stopButton.setDisable(false);
                cameraSelector.setDisable(true);
                statusLabel.setText("Camera is running");
                isScanning = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Camera error: " + e.getMessage());
        }
    }

    @FXML
    protected void onStopButtonClick() {
        if (timer != null) {
            timer.stop();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        imageView.setImage(null);
        startButton.setDisable(false);
        stopButton.setDisable(true);
        cameraSelector.setDisable(false);
        statusLabel.setText("Camera is off");
        isScanning = false;
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private String decodeQRCode(BufferedImage image) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        Result result = reader.decode(bitmap);
        return result.getText();
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
        
        return image;
    }

    @FXML
    protected void copyResult() {
        String result = resultLabel.getText();
        if (result != null && !result.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(result);
            clipboard.setContent(content);
            statusLabel.setText("Result copied to clipboard!");
        }
    }
}