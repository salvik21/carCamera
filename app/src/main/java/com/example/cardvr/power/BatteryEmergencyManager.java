package com.example.cardvr.power;

public final class BatteryEmergencyManager {
    public enum Level { NORMAL, LOW, CRITICAL }

    public Level evaluate(int batteryPercent, boolean charging) {
        if (charging || batteryPercent < 0) return Level.NORMAL;
        if (batteryPercent <= 5) return Level.CRITICAL;
        if (batteryPercent <= 20) return Level.LOW;
        return Level.NORMAL;
    }
}
