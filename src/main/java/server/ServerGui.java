package server;

import com.weatherclientapp.common.WeatherService; // Đảm bảo import đúng package của bạn
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerGui extends Application {

    // Thay TextArea bằng TextFlow để hỗ trợ màu sắc
    private TextFlow logFlow;
    private ScrollPane scrollPane;

    private Button toggleBtn;
    private Label statusLabel;
    private boolean isRunning = false;

    // Các thành phần Server
    private Registry registry;
    private WeatherServiceImpl rmiService;
    private UdpChatServer udpServer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hệ thống Quản trị Server (WeatherMonitor Pro)");

        // --- GIAO DIỆN CHÍNH ---
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1e1e1e;"); // Nền đen thẫm (Dark Mode)

        // 1. Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 15, 0));
        header.setStyle("-fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label title = new Label("SERVER CONSOLE");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#61dafb")); // Màu xanh React đẹp

        statusLabel = new Label("OFFLINE");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setPadding(new Insets(4, 10, 4, 10));
        statusLabel.setStyle("-fx-border-color: gray; -fx-border-radius: 10; -fx-border-width: 2;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleBtn = new Button("▶ START SERVER");
        toggleBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        toggleBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        toggleBtn.setPrefHeight(35);
        toggleBtn.setOnAction(e -> toggleServer());

        header.getChildren().addAll(title, statusLabel, spacer, toggleBtn);

        // 2. Log Area (TextFlow bên trong ScrollPane)
        logFlow = new TextFlow();
        logFlow.setStyle("-fx-background-color: #1e1e1e;");
        logFlow.setPadding(new Insets(10));
        // Giãn dòng logFlow
        logFlow.setLineSpacing(5);

        scrollPane = new ScrollPane(logFlow);
        scrollPane.setFitToWidth(true); // Tự động xuống dòng
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        // Tự động cuộn xuống dưới cùng
        logFlow.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1.0));

        VBox centerBox = new VBox(10, scrollPane);
        VBox.setVgrow(centerBox, Priority.ALWAYS); // Chiếm hết chỗ trống

        root.setTop(header);
        root.setCenter(centerBox);

        // --- MAGIC: CHUYỂN HƯỚNG SYSTEM.OUT ---
        redirectSystemStreams();

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> stopServer());
        primaryStage.show();
    }

    // ========================================================
    // LOGIC SERVER
    // ========================================================
    private void toggleServer() {
        if (!isRunning) startServer();
        else stopServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                System.out.println(">>> Đang khởi động hệ thống...");

                // 1. RMI
                try {
                    registry = LocateRegistry.createRegistry(1099);
                } catch (Exception e) {
                    registry = LocateRegistry.getRegistry(1099);
                }

                rmiService = new WeatherServiceImpl();
                registry.rebind("WeatherSystem", rmiService);
                System.out.println("✅ RMI Server: OK (Port 1099)");

                // 2. UDP
                udpServer = new UdpChatServer();
                udpServer.start();
                System.out.println("✅ UDP Chatbot: OK (Port 9876)");

                updateStatus(true);
                System.out.println(">>> SERVER ĐÃ SẴN SÀNG PHỤC VỤ!");

            } catch (Exception e) {
                System.err.println("❌ Lỗi khởi động: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void stopServer() {
        new Thread(() -> {
            try {
                System.out.println(">>> Đang tắt hệ thống...");
                if (registry != null) try { registry.unbind("WeatherSystem"); } catch (Exception ignored) {}
                if (rmiService != null) UnicastRemoteObject.unexportObject(rmiService, true);
                if (udpServer != null) udpServer.stopServer();

                updateStatus(false);
                System.out.println(">>> Server đã tắt hoàn toàn.");
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi tắt: " + e.getMessage());
            }
        }).start();
    }

    private void updateStatus(boolean running) {
        isRunning = running;
        Platform.runLater(() -> {
            if (running) {
                statusLabel.setText("ONLINE");
                statusLabel.setTextFill(Color.LIGHTGREEN);
                statusLabel.setStyle("-fx-border-color: lightgreen; -fx-border-radius: 10; -fx-border-width: 2;");
                toggleBtn.setText("⏹ STOP SERVER");
                toggleBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
            } else {
                statusLabel.setText("OFFLINE");
                statusLabel.setTextFill(Color.GRAY);
                statusLabel.setStyle("-fx-border-color: gray; -fx-border-radius: 10; -fx-border-width: 2;");
                toggleBtn.setText("▶ START SERVER");
                toggleBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 5;");
            }
        });
    }

    // ========================================================
    // LOGIC TÔ MÀU LOG (QUAN TRỌNG NHẤT)
    // ========================================================
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                // Không xử lý từng byte lẻ để tránh lỗi hiển thị
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String msg = new String(b, off, len).trim();
                if (!msg.isEmpty()) {
                    Platform.runLater(() -> appendColoredLog(msg));
                }
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void appendColoredLog(String msg) {
        // 1. Tạo timestamp
        Text timeText = new Text("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] ");
        timeText.setFill(Color.GRAY);
        timeText.setFont(Font.font("Consolas", 12));

        // 2. Tạo nội dung log và tô màu dựa trên từ khóa
        Text contentText = new Text(msg + "\n");
        contentText.setFont(Font.font("Consolas", 14));

        if (msg.contains("❌") || msg.contains("Error") || msg.contains("Exception")) {
            contentText.setFill(Color.web("#ff5555")); // Đỏ
        } else if (msg.contains("✅") || msg.contains("SẴN SÀNG")) {
            contentText.setFill(Color.web("#50fa7b")); // Xanh lá
        } else if (msg.contains("UDP") || msg.contains("Chat")) {
            contentText.setFill(Color.web("#8be9fd")); // Xanh dương (Cyan)
        } else if (msg.contains("RMI") || msg.contains("MONITOR")) {
            contentText.setFill(Color.web("#f1fa8c")); // Vàng
        } else {
            contentText.setFill(Color.web("#f8f8f2")); // Trắng ngà
        }

        // 3. Thêm vào giao diện
        logFlow.getChildren().addAll(timeText, contentText);
    }
}