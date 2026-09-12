package de.andreasgerhard.pcblaserstudio.service.serial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GrblStatusTest {

    @Test
    void parsesIdleStatusWithMpos() {
        GrblStatus s = GrblStatus.parse("<Idle|MPos:1.000,2.500,0.000|FS:0,0>");
        assertNotNull(s);
        assertEquals("Idle", s.state());
        assertEquals(1.0, s.mpos()[0], 1e-9);
        assertEquals(2.5, s.mpos()[1], 1e-9);
        assertEquals(0.0, s.feed(), 1e-9);
        assertEquals(0.0, s.spindle(), 1e-9);
    }

    @Test
    void parsesRunStatusWithWposAndFeed() {
        GrblStatus s = GrblStatus.parse("<Run|WPos:5.500,6.250,0.000|FS:1200,750>");
        assertNotNull(s);
        assertEquals("Run", s.state());
        assertNull(s.mpos());
        assertEquals(5.5, s.wpos()[0], 1e-9);
        assertEquals(1200, s.feed(), 1e-9);
        assertEquals(750, s.spindle(), 1e-9);
    }

    @Test
    void rejectsNonStatusLines() {
        assertNull(GrblStatus.parse("ok"));
        assertNull(GrblStatus.parse("error:9"));
        assertNull(GrblStatus.parse("ALARM:1"));
        assertNull(GrblStatus.parse(null));
        assertNull(GrblStatus.parse(""));
    }

    @Test
    void toleratesMissingFields() {
        GrblStatus s = GrblStatus.parse("<Alarm|>");
        assertNotNull(s);
        assertEquals("Alarm", s.state());
        assertNull(s.mpos());
        assertEquals("-", s.positionText());
        assertEquals("-", s.feedText());
    }
}
