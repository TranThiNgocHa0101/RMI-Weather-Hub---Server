package server;

import com.weatherclientapp.common.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class OpenWeatherMapHandler {

    // ⚠️ QUAN TRỌNG: Hãy thay dòng bên dưới bằng API Key của bạn
    private static final String API_KEY = "cf5c1dc3b89d213445dc0f3c1de3a3c4";

    // URL lấy thời tiết hiện tại
    private static final String CURRENT_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=vi";

    // URL lấy dự báo 5 ngày / 3 giờ
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=vi";

    // --- HÀM 1: LẤY THỜI TIẾT HIỆN TẠI ---
    public static WeatherData getRealWeather(String city) throws IOException, InterruptedException {
        // 1. Xử lý tên thành phố (thay khoảng trắng bằng %20)
        String url = String.format(CURRENT_URL, city.trim().replace(" ", "%20"), API_KEY);

        // 2. Gửi yêu cầu HTTP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 3. Kiểm tra lỗi (Nếu không phải 200 OK thì ném lỗi)
        if (response.statusCode() != 200) {
            throw new IOException("Lỗi API (Mã " + response.statusCode() + "): Không tìm thấy thành phố hoặc sai API Key.");
        }

        // 4. Phân tích JSON trả về
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());

        // Lấy các trường dữ liệu cần thiết
        String cityName = rootNode.path("name").asText();
        double temp = rootNode.path("main").path("temp").asDouble();
        double humidity = rootNode.path("main").path("humidity").asDouble();

        // Lấy mảng weather (phần tử đầu tiên)
        JsonNode weatherNode = rootNode.path("weather").get(0);
        String description = weatherNode.path("description").asText(); // VD: "mây cụm"
        String iconCode = weatherNode.path("icon").asText();           // VD: "04d"

        // [MỚI] Lấy lượng mưa hiện tại (Rain volume for last 1 hour)
        // JSON thường có dạng: "rain": { "1h": 2.5 }
        double rain = 0.0;
        if (rootNode.has("rain")) {
            JsonNode rainNode = rootNode.get("rain");
            if (rainNode.has("1h")) {
                rain = rainNode.get("1h").asDouble();
            }
        }

        // 5. Tạo Object và gán mưa
        WeatherData data = new WeatherData(cityName, temp, humidity, description, iconCode);
        data.setRainVolume(rain); // Gán lượng mưa vào

        return data;
    }

    // --- HÀM 2: LẤY DANH SÁCH DỰ BÁO (CHO BIỂU ĐỒ) ---
    public static List<WeatherData> getForecastList(String city) throws IOException, InterruptedException {
        String url = String.format(FORECAST_URL, city.trim().replace(" ", "%20"), API_KEY);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Lỗi API Forecast: " + response.statusCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.body());

        // API trả về một mảng tên là "list" chứa 40 điểm dữ liệu (5 ngày x 8 lần/ngày)
        JsonNode listNode = rootNode.path("list");
        String cityName = rootNode.path("city").path("name").asText();

        List<WeatherData> forecastList = new ArrayList<>();

        // Chúng ta chỉ lấy 8 điểm dữ liệu đầu tiên (tương đương 24 giờ tới) để vẽ biểu đồ cho đẹp
        int limit = 8;
        if (listNode.size() < 8) limit = listNode.size();

        for (int i = 0; i < limit; i++) {
            JsonNode item = listNode.get(i);

            double temp = item.path("main").path("temp").asDouble();
            double humidity = item.path("main").path("humidity").asDouble();
            String icon = item.path("weather").get(0).path("icon").asText();

            // Lấy thời gian dự báo (VD: "2025-12-08 15:00:00")
            String fullTime = item.path("dt_txt").asText();

            // Cắt chuỗi để lấy giờ cho gọn (VD: lấy "15:00" từ chuỗi trên)
            // Ký tự thứ 11 đến 16 là giờ và phút
            String shortTime = fullTime.substring(11, 16);

            // [MỚI] Lấy lượng mưa dự báo (Rain volume for last 3 hours)
            // JSON dự báo thường có dạng: "rain": { "3h": 5.2 }
            double rain = 0.0;
            if (item.has("rain")) {
                JsonNode rainNode = item.get("rain");
                if (rainNode.has("3h")) {
                    rain = rainNode.get("3h").asDouble();
                }
            }

            // Tạo đối tượng WeatherData
            // MẸO: Ta lưu "Giờ" vào trường "description" để Client dễ dàng lấy ra vẽ trục X của biểu đồ
            WeatherData data = new WeatherData(cityName, temp, humidity, shortTime, icon);
            data.setRainVolume(rain); // Gán lượng mưa vào

            forecastList.add(data);
        }

        return forecastList;
    }
}