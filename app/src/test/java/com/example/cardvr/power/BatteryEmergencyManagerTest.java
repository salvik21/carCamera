package com.example.cardvr.power;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BatteryEmergencyManagerTest {
    @Test
    public void evaluate_mapsBatteryLevels() {
        BatteryEmergencyManager manager = new BatteryEmergencyManager();

        assertEquals(BatteryEmergencyManager.Level.NORMAL, manager.evaluate(50, false));
        assertEquals(BatteryEmergencyManager.Level.LOW, manager.evaluate(20, false));
        assertEquals(BatteryEmergencyManager.Level.CRITICAL, manager.evaluate(5, false));
        assertEquals(BatteryEmergencyManager.Level.NORMAL, manager.evaluate(3, true));
    }
}
