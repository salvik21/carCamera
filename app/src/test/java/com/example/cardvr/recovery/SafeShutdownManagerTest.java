package com.example.cardvr.recovery;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SafeShutdownManagerTest {
    @Test
    public void begin_allowsOnlyFirstShutdown() {
        SafeShutdownManager manager = new SafeShutdownManager();

        assertTrue(manager.begin());
        assertTrue(manager.isShuttingDown());
        assertFalse(manager.begin());
    }
}
