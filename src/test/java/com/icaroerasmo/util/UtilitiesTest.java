package com.icaroerasmo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilitiesTest {

    private final Utilities utilities = new Utilities();

    @Test
    void formatsZeroMillisAsEmptyString() {
        assertEquals("", utilities.getFullTimeAmount(0));
    }

    @Test
    void formatsSingularUnits() {
        assertEquals("1 second", utilities.getFullTimeAmount(1000));
        assertEquals("1 millisecond", utilities.getFullTimeAmount(1));
    }

    @Test
    void formatsPluralUnits() {
        assertEquals("2 seconds", utilities.getFullTimeAmount(2000));
        assertEquals("2 milliseconds", utilities.getFullTimeAmount(2));
    }

    @Test
    void formatsCombinedUnits() {
        // 1 day + 1 hour + 1 minute + 1 second = 90.061.000 ms
        assertEquals("1 day, 1 hour, 1 minute, 1 second",
                utilities.getFullTimeAmount(90061000));
        assertEquals("1 minute, 1 second, 1 millisecond",
                utilities.getFullTimeAmount(61001));
        assertEquals("1 second, 500 milliseconds",
                utilities.getFullTimeAmount(1500));
    }

    @Test
    void formatsMultipleDaysAndHours() {
        // 2 days + 3 hours = 183.600.000 ms
        assertEquals("2 days, 3 hours", utilities.getFullTimeAmount(183600000));
    }

    @Test
    void trimsTrailingSeparatorWhenHigherUnitsMissing() {
        // 0 days/hours/minutes, 0 seconds, 500 ms
        assertEquals("500 milliseconds", utilities.getFullTimeAmount(500));
    }

    @Test
    void killProcessIsNullSafe() {
        utilities.killProcess(null);
    }
}
