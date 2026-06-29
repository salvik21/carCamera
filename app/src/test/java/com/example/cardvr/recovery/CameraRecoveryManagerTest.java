package com.example.cardvr.recovery;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CameraRecoveryManagerTest {
    @Test
    public void nextDelay_usesBackoffAndThenStops() {
        CameraRecoveryManager manager = new CameraRecoveryManager();

        assertEquals(1_000L, manager.nextDelayMillis());
        assertEquals(3_000L, manager.nextDelayMillis());
        assertEquals(10_000L, manager.nextDelayMillis());
        assertEquals(60_000L, manager.nextDelayMillis());
        assertFalse(manager.shouldStopRecording());
        assertEquals(60_000L, manager.nextDelayMillis());
        assertTrue(manager.shouldStopRecording());
    }
}
