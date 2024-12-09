package org.example.qrcodescanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AnimatedQRCodeScanner extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            URL fxmlUrl = AnimatedQRCodeScanner.class.getResource("/org/example/qrcodescanner/hello-view.fxml");
            if (fxmlUrl == null) {
                throw new IOException("Cannot find FXML file");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("QR Code Scanner");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading FXML: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}