package com.weatherclientapp.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WeatherCallback extends Remote {
    // Hàm này Server sẽ gọi, nhưng code chạy ở Client
    void onEmergencyAlert(String message) throws RemoteException;
}