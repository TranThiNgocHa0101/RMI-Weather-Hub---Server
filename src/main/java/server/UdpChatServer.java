package server;

import com.weatherclientapp.common.WeatherData;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UdpChatServer extends Thread {
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[4096];

    // 1. TỪ ĐIỂN THÀNH PHỐ
    private static final Map<String, String> CITY_DICTIONARY = new HashMap<>();
    static {
        CITY_DICTIONARY.put("hà nội", "Hanoi"); CITY_DICTIONARY.put("hn", "Hanoi");
        CITY_DICTIONARY.put("hồ chí minh", "Ho Chi Minh City"); CITY_DICTIONARY.put("sài gòn", "Ho Chi Minh City"); CITY_DICTIONARY.put("tphcm", "Ho Chi Minh City");
        CITY_DICTIONARY.put("đà nẵng", "Da Nang"); CITY_DICTIONARY.put("đn", "Da Nang");
        CITY_DICTIONARY.put("huế", "Hue"); CITY_DICTIONARY.put("cần thơ", "Can Tho");
        CITY_DICTIONARY.put("nha trang", "Nha Trang"); CITY_DICTIONARY.put("đà lạt", "Da Lat");
        CITY_DICTIONARY.put("sapa", "Sapa"); CITY_DICTIONARY.put("vinh", "Vinh");
        CITY_DICTIONARY.put("quy nhơn", "Qui Nhon");
    }

    // 2. BỘ NHỚ NGỮ CẢNH
    private final Map<String, String> userContext = new HashMap<>();

    public UdpChatServer() {
        try {
            socket = new DatagramSocket(9876);
            System.out.println(">>> [UDP-AI] Chatbot V2.0 (Nhạy bén hơn) đã chạy tại cổng 9876...");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void run() {
        running = true;
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                String userKey = address.toString() + ":" + port;

                System.out.println("[UDP] " + userKey + " hỏi: " + received);

                String response = processSmartBot(received, userKey);

                byte[] sendBuf = response.getBytes(StandardCharsets.UTF_8);
                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
                socket.send(sendPacket);

            } catch (Exception e) { e.printStackTrace(); }
        }
        socket.close();
    }

    // =================================================================
    // BỘ NÃO XỬ LÝ TRUNG TÂM
    // =================================================================
    private String processSmartBot(String msg, String userKey) {
        String lowerMsg = msg.toLowerCase();

        // BƯỚC 1: Phân tích chủ đề (Intent Detection) - QUAN TRỌNG NHẤT
        String newIntent = detectIntent(lowerMsg);

        // Debug: In ra xem Bot hiểu là gì
        System.out.println("   -> Intent phát hiện: " + newIntent);

        // Logic cập nhật bộ nhớ:
        // - Nếu phát hiện chủ đề mới (VD: travel) -> Ghi đè bộ nhớ ngay.
        // - Nếu là "none" (không rõ) -> Dùng lại chủ đề cũ trong bộ nhớ.
        // - Nếu là "reset" (hỏi chung chung) -> Xóa bộ nhớ để trả lời mặc định.

        String finalIntent = "none";

        if (!newIntent.equals("none") && !newIntent.equals("reset")) {
            userContext.put(userKey, newIntent); // Cập nhật chủ đề mới
            finalIntent = newIntent;
        } else if (newIntent.equals("reset")) {
            userContext.remove(userKey); // Xóa ngữ cảnh cũ
            finalIntent = "none";
        } else {
            // Dùng lại ngữ cảnh cũ nếu người dùng không đổi chủ đề
            if (userContext.containsKey(userKey)) {
                finalIntent = userContext.get(userKey);
            }
        }

        // BƯỚC 2: Tìm tên thành phố
        String cityCode = null;
        for (Map.Entry<String, String> entry : CITY_DICTIONARY.entrySet()) {
            if (lowerMsg.contains(entry.getKey())) {
                cityCode = entry.getValue();
                break;
            }
        }

        // BƯỚC 3: Trả lời
        if (lowerMsg.contains("hello") || lowerMsg.contains("chào")) {
            return "Bot: Chào bạn! Tôi có thể tư vấn về: Giặt đồ, Du lịch, Sức khỏe...";
        }

        if (cityCode != null) {
            try {
                WeatherData data = OpenWeatherMapHandler.getRealWeather(cityCode);
                return analyzeByIntent(data, finalIntent);
            } catch (Exception e) {
                return "Bot: 📡 Mất kết nối vệ tinh với " + cityCode + " rồi!";
            }
        }

        return "Bot: Bạn muốn hỏi về thành phố nào? (Ví dụ: 'Hà Nội có mưa không?')";
    }

    // --- CẢI TIẾN: Hàm phát hiện ý định với nhiều từ khóa hơn ---
    private String detectIntent(String msg) {
        // 1. Nhóm Giặt Là
        if (msg.contains("giặt") || msg.contains("phơi") || msg.contains("quần") ||
                msg.contains("áo") || msg.contains("khô") || msg.contains("ướt") || msg.contains("lâu"))
            return "laundry";

        // 2. Nhóm Du Lịch / Đi Chơi (Thêm nhiều từ lóng)
        if (msg.contains("chơi") || msg.contains("lịch") || msg.contains("dạo") ||
                msg.contains("phố") || msg.contains("cafe") || msg.contains("café") ||
                msg.contains("ra ngoài") || msg.contains("đi đâu") || msg.contains("vivu"))
            return "travel";

        // 3. Nhóm Sức Khỏe
        if (msg.contains("khỏe") || msg.contains("ốm") || msg.contains("mệt") ||
                msg.contains("cảm") || msg.contains("ấm") || msg.contains("lạnh") || msg.contains("mặc"))
            return "health";

        // 4. Nhóm Reset (Hỏi chung chung -> Muốn xem tổng quan)
        if (msg.equals("thời tiết") || msg.contains("thế nào") || msg.contains("ra sao") || msg.contains("như nào")) {
            // Nếu chỉ hỏi "Hà Nội thế nào" mà không có từ khóa khác -> Coi như reset về xem chung
            return "reset";
        }

        return "none"; // Không phát hiện gì đặc biệt
    }

    // --- Hàm phân tích chuyên sâu ---
    private String analyzeByIntent(WeatherData data, String intent) {
        double temp = data.getTemperature();
        double hum = data.getHumidity();
        String desc = data.getDescription();
        String baseInfo = String.format("🌍 %s: %.1f°C | %s | 💧%.0f%%", data.getCity(), temp, desc, hum);

        switch (intent) {
            case "laundry":
                String laundryAdvice;
                if (desc.contains("mưa")) laundryAdvice = "❌ Đang mưa! Đừng phơi đồ.";
                else if (hum > 85) laundryAdvice = "⚠️ Ẩm rất cao (>85%). Quần áo sẽ hôi, nên sấy.";
                else if (hum > 70) laundryAdvice = "✅ Phơi được, nhưng sẽ hơi lâu khô.";
                else laundryAdvice = "☀️ Trời khô ráo. Giặt phơi thoải mái!";
                return baseInfo + "\n👕 GIẶT LÀ: " + laundryAdvice;

            case "health":
                String healthAdvice;
                if (temp < 15) healthAdvice = "❄️ Trời lạnh. Nhớ mặc áo ấm, quàng khăn.";
                else if (temp > 35) healthAdvice = "🔥 Nắng nóng gay gắt! Coi chừng sốc nhiệt.";
                else if (temp > 28 && hum > 80) healthAdvice = "😓 Trời oi bức. Uống nhiều nước nhé.";
                else healthAdvice = "💪 Thời tiết lý tưởng cho sức khỏe.";
                return baseInfo + "\n❤️ SỨC KHỎE: " + healthAdvice;

            case "travel":
                String travelAdvice;
                if (desc.contains("mưa") || desc.contains("dông")) travelAdvice = "☔ Đang mưa/dông. Nên hoãn đi chơi xa.";
                else if (temp > 34) travelAdvice = "☀️ Nắng gắt. Nên đi chơi lúc chiều tối hoặc vào Mall.";
                else if (temp >= 18 && temp <= 29) travelAdvice = "🚗 Thời tiết tuyệt vời để đi dạo phố!";
                else travelAdvice = "☁️ Trời ổn, đi chơi được.";
                return baseInfo + "\n🚗 ĐI CHƠI: " + travelAdvice;

            default: // Mặc định (hoặc khi Reset)
                return baseInfo + "\n🤖 (Tôi đang hiện thông tin chung. Bạn có thể hỏi: 'có phơi đồ được không?', 'đi chơi được không?')\n" + getGeneralAdvice(temp, hum);
        }
    }

    private String getGeneralAdvice(double t, double h) {
        if(t > 35) return "🔥 Lưu ý: Trời rất nóng.";
        if(h > 90) return "💧 Lưu ý: Độ ẩm rất cao.";
        return "✅ Thời tiết bình thường.";
    }
    public void stopServer() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println(">>> [UDP] Đã đóng cổng Chat.");
    }
}