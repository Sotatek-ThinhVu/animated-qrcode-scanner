module org.example.qrcodescanner {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires opencv;
    requires com.google.zxing;
    requires java.desktop;
    requires com.google.zxing.javase;
    requires com.sparrowwallet.hummingbird;
    requires jdk.compiler;

    opens org.example.qrcodescanner to javafx.fxml;
    exports org.example.qrcodescanner;
}