package com.example.cardvr.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SpeedCalculatorTest {
    @Test public void convertsMetersPerSecond() {
        assertEquals(36.0, SpeedCalculator.metersPerSecondToKmh(10), 0.001);
    }

    @Test public void rejectsInvalidInput() {
        assertEquals(0, SpeedCalculator.metersPerSecondToKmh(-1), 0);
        assertEquals(0, SpeedCalculator.metersPerSecondToKmh(Double.NaN), 0);
    }
}
