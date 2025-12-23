package server;

import com.weatherclientapp.common.WeatherService; // Import của bạn
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) {
        try {
            System.out.println(">>> ĐANG KHỞI ĐỘNG HỆ THỐNG HYBRID (RMI + UDP)...");

            // ====================================================
            // PHẦN 1: KHỞI ĐỘNG RMI (Cho Tra cứu Thời tiết & Cảnh báo)
            // ====================================================

            // 1. Tạo thanh ghi RMI (Registry) tại cổng 1099/ mở IP
            System.setProperty("java.rmi.server.hostname", " 192.168.43.7");

            // Tạo registry mới trên port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

             // Tạo đối tượng Service
            WeatherService service = new WeatherServiceImpl();

             // Bind service lên registry
            registry.rebind("WeatherSystem", service);

            System.out.println("✅ RMI SERVER đã chạy trên  192.168.43.7:1099");


            // ====================================================
            // PHẦN 2: KHỞI ĐỘNG UDP (Cho Chatbot) - MỚI THÊM VÀO
            // ====================================================

            // Khởi tạo và chạy luồng UDP Server riêng biệt (Cổng 9876)
            // Lưu ý: Class UdpChatServer phải extends Thread như hướng dẫn trước
            new UdpChatServer().start();

            System.out.println("✅ UDP SERVER: Đã chạy tại cổng 9876 (Dùng cho Chatbot)");

            // ====================================================
            System.out.println("------------------------------------------");
            System.out.println(">>> TOÀN BỘ SERVER ĐÃ SẴN SÀNG PHỤC VỤ!");
            System.out.println("------------------------------------------");

        } catch (Exception e) {
            System.err.println("❌ Lỗi khởi động Server: " + e.getMessage());
            e.printStackTrace();
            // Nếu cổng bị kẹt, hãy tắt các tiến trình Java trong Task Manager rồi chạy lại
        }
    }
}