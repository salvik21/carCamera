package com.example.cardvr.diagnostics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;

import com.example.cardvr.cloud.CloudAuthenticationManager;
import com.example.cardvr.cloud.CloudUploadRepository;
import com.example.cardvr.power.BatteryMonitor;
import com.example.cardvr.power.ChargingStateManager;
import com.example.cardvr.power.ThermalMonitor;
import com.example.cardvr.storage.StorageManager;

import java.util.ArrayList;
import java.util.List;

public final class AppHealthChecker {
    public enum Status { READY, WARNING, ERROR }

    private final Context context;

    public AppHealthChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Item> check() {
        List<Item> items = new ArrayList<>();
        items.add(item("Камера", has(Manifest.permission.CAMERA) ? Status.READY : Status.ERROR,
                "Разрешение камеры"));
        items.add(item("Микрофон", has(Manifest.permission.RECORD_AUDIO) ? Status.READY : Status.WARNING,
                "Без микрофона видео будет без звука"));
        items.add(item("GPS", has(Manifest.permission.ACCESS_FINE_LOCATION)
                        || has(Manifest.permission.ACCESS_COARSE_LOCATION) ? Status.READY : Status.WARNING,
                "Скорость и поездки ограничены без геолокации"));
        items.add(item("Память", new StorageManager(context).getAvailableBytes() > 512L * 1024L * 1024L
                ? Status.READY : Status.WARNING, "Свободное место"));
        try {
            com.example.cardvr.database.AppDatabase.getInstance(context).getOpenHelper().getReadableDatabase();
            items.add(item("База данных", Status.READY, "Room доступна"));
        } catch (Exception e) {
            items.add(item("База данных", Status.ERROR, e.getMessage()));
        }
        items.add(item("Foreground Service", Status.READY, "Разрешения объявлены в Manifest"));
        items.add(item("Облако", new CloudAuthenticationManager(context).isConnected()
                ? Status.READY : Status.WARNING, "Google Drive"));
        items.add(item("Очередь загрузки", new CloudUploadRepository(context).countActiveTasks() == 0
                ? Status.READY : Status.WARNING, "Есть ожидающие задачи"));
        ChargingStateManager.ChargingState battery = BatteryMonitor.readCurrentState(context);
        items.add(item("Зарядка", battery.isCharging() ? Status.READY : Status.WARNING,
                battery.getBatteryPercent() + "%"));
        items.add(item("Температура", thermalStatus(new ThermalMonitor(context).currentLevel()),
                "Текущее thermal-состояние"));
        try {
            WorkManager.getInstance(context);
            items.add(item("WorkManager", Status.READY, "Доступен"));
        } catch (Exception e) {
            items.add(item("WorkManager", Status.ERROR, e.getMessage()));
        }
        return items;
    }

    private boolean has(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static Status thermalStatus(ThermalMonitor.Level level) {
        switch (level) {
            case CRITICAL:
            case HOT: return Status.ERROR;
            case WARM: return Status.WARNING;
            case NORMAL:
            default: return Status.READY;
        }
    }

    private static Item item(String name, Status status, String detail) {
        Item item = new Item();
        item.name = name;
        item.status = status;
        item.detail = detail == null ? "" : detail;
        return item;
    }

    public static final class Item {
        public String name;
        public Status status;
        public String detail;
    }
}
