package server;

import com.weatherclientapp.common.WeatherCallback;
import com.weatherclientapp.common.WeatherData;
import com.weatherclientapp.common.WeatherService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class WeatherServiceImpl extends UnicastRemoteObject implements WeatherService {

    // --- CẤU HÌNH ---
    private static final Map<String, CachedWeather> currentCache = new HashMap<>();
    private static final long CACHE_DURATION = 10 * 60 * 1000;

    private static final String[] MONITOR_CITIES = {
            "Hanoi", "Ho Chi Minh City", "Da Nang", "Sapa", "Can Tho", "Hue"
    };

    private final List<WeatherCallback> clients = new CopyOnWriteArrayList<>();

    public WeatherServiceImpl() throws RemoteException {
        super();
        startRealTimeMonitor();
    }

    // --- LOGIC GIÁM SÁT THIÊN TAI (GIỮ NGUYÊN) ---
    private void startRealTimeMonitor() {
        new Thread(() -> {
            System.out.println(">>> [SYSTEM] Hệ thống Phân tích Thiên tai THỰC TẾ đã khởi động...");
            int index = 0;
            while (true) {
                try {
                    Thread.sleep(20000);
                    String city = MONITOR_CITIES[index % MONITOR_CITIES.length];
                    index++;

                    WeatherData data = OpenWeatherMapHandler.getRealWeather(city);
                    System.out.printf("[MONITOR] Đang phân tích: %s (T: %.1f°C | H: %.0f%%)\n",
                            data.getCity(), data.getTemperature(), data.getHumidity());

                    String alertPayload = analyzeWeather(data);

                    if (alertPayload != null && !clients.isEmpty()) {
                        notifyAllClients(alertPayload);
                    }
                } catch (Exception e) {
                    System.err.println("[MONITOR ERROR] " + e.getMessage());
                }
            }
        }).start();
    }

    private String analyzeWeather(WeatherData data) {
        double temp = data.getTemperature();
        double hum = data.getHumidity();
        String city = data.getCity();

        if (temp >= 37.0) return "🔥 CẢNH BÁO NẮNG NÓNG CỰC ĐOAN|Tại " + city + " nhiệt độ > 37°C.";
        else if (hum >= 95.0) return "🌧️ CẢNH BÁO MƯA LỚN|Tại " + city + " độ ẩm bão hòa " + hum + "%.";
        else if (temp <= 10.0) return "❄️ CẢNH BÁO RÉT HẠI|Tại " + city + " nhiệt độ giảm sâu " + temp + "°C.";

        return null;
    }

    private void notifyAllClients(String message) {
        for (WeatherCallback client : clients) {
            try { client.onEmergencyAlert(message); }
            catch (RemoteException e) { clients.remove(client); }
        }
    }

    // --- CÁC HÀM RMI BẮT BUỘC (GIỮ NGUYÊN) ---
    @Override
    public void registerForAlerts(WeatherCallback client) throws RemoteException {
        if (!clients.contains(client)) {
            clients.add(client);
            try { client.onEmergencyAlert("📡 CONNECT SUCCESS |Hệ thống giám sát đã kích hoạt."); }
            catch (Exception e) { clients.remove(client); }
        }
    }

    @Override
    public void unregisterForAlerts(WeatherCallback client) throws RemoteException {
        clients.remove(client);
    }

    @Override
    public WeatherData getWeatherInformation(String city) throws RemoteException {
        String cityKey = city.trim().toLowerCase();
        if (currentCache.containsKey(cityKey)) {
            CachedWeather cached = currentCache.get(cityKey);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) return cached.data;
        }
        try {
            WeatherData realData = OpenWeatherMapHandler.getRealWeather(city);
            currentCache.put(cityKey, new CachedWeather(realData, System.currentTimeMillis()));
            return realData;
        } catch (Exception e) {
            return new WeatherData(city + " (Lỗi)", 0, 0, "Mất kết nối", "01d");
        }
    }

    @Override
    public List<WeatherData> getForecast(String city) throws RemoteException {
        try { return OpenWeatherMapHandler.getForecastList(city); }
        catch (Exception e) { return new ArrayList<>(); }
    }



    // --- ĐÃ XÓA HÀM chatWithBot VÌ ĐÃ CHUYỂN QUA UDP ---

    private static class CachedWeather {
        WeatherData data; long timestamp;
        CachedWeather(WeatherData d, long t) { data = d; timestamp = t; }
    }
}