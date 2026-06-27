package com.example.cardvr.trips;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TripEndDetectorTest {
    private final TripEndDetector detector = new TripEndDetector();

    @Test public void shortStopDoesNotEndTrip() {
        long now = 1_000_000L;
        assertFalse(detector.shouldSuggestEnd(false, 0, now - 60_000, true,
                true, now - 60_000, 300_000, 600_000, now));
    }

    @Test public void requiresPowerAndStoppedDelays() {
        long now = 1_000_000L;
        assertTrue(detector.shouldSuggestEnd(false, 0, now - 700_000, true,
                true, now - 400_000, 300_000, 600_000, now));
        assertFalse(detector.shouldSuggestEnd(true, 0, now - 700_000, true,
                true, 0, 300_000, 600_000, now));
    }

    @Test public void gpsLossDoesNotEndTrip() {
        long now = 1_000_000L;
        assertFalse(detector.shouldSuggestEnd(false, 0, now - 700_000, false,
                true, now - 400_000, 300_000, 600_000, now));
    }

    @Test public void inactiveRecordingDoesNotTriggerEnd() {
        long now = 1_000_000L;
        assertFalse(detector.shouldSuggestEnd(false, 0, now - 700_000, true,
                false, now - 400_000, 300_000, 600_000, now));
    }
}
