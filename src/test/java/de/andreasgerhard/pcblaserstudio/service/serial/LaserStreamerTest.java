package de.andreasgerhard.pcblaserstudio.service.serial;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaserStreamerTest {

    private static final String GCODE =
            "G21\n" +
            "G90\n" +
            "; a comment line\n" +
            "\n" +
            "G0 X0 Y0\n" +
            "G1 X10 Y0 F3000\n" +
            "M5\n";

    @Test
    void parseCommandsDropsBlanksAndComments() {
        List<String> cmds = LaserStreamer.parseCommands(GCODE);
        assertEquals(5, cmds.size());
        assertEquals("G21", cmds.get(0));
        assertEquals("M5", cmds.get(4));
    }

    @Test
    void streamsAllCommandsToCompletion() throws Exception {
        SimulatedLaserConnection conn = new SimulatedLaserConnection();
        conn.open();
        try {
            AtomicReference<LaserStreamer.Result> result = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            LaserStreamer streamer = new LaserStreamer(conn, GCODE,
                    new LaserStreamer.Listener() {
                        @Override
                        public void onFinished(LaserStreamer.Result r) {
                            result.set(r);
                            done.countDown();
                        }
                    });

            assertEquals(5, streamer.getTotalLines());
            streamer.start();
            assertTrue(done.await(10, TimeUnit.SECONDS), "streaming did not finish in time");
            assertEquals(LaserStreamer.State.FINISHED, result.get().finalState());
            assertEquals(5, result.get().ackedLines());
            assertTrue(result.get().isSuccess());
        } finally {
            conn.close();
        }
    }

    @Test
    void stopCancelsARunningJob() throws Exception {
        SimulatedLaserConnection conn = new SimulatedLaserConnection();
        conn.open();
        try {
            StringBuilder gcode = new StringBuilder();
            for (int i = 0; i < 2000; i++) {
                gcode.append("G1 X").append(i).append(" Y0 F3000\n");
            }

            AtomicReference<LaserStreamer.Result> result = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            LaserStreamer streamer = new LaserStreamer(conn, gcode.toString(),
                    new LaserStreamer.Listener() {
                        @Override
                        public void onFinished(LaserStreamer.Result r) {
                            result.set(r);
                            done.countDown();
                        }
                    });

            streamer.start();
            Thread.sleep(50);
            streamer.stop();

            assertTrue(done.await(10, TimeUnit.SECONDS), "streamer did not stop in time");
            assertEquals(LaserStreamer.State.CANCELLED, result.get().finalState());
        } finally {
            conn.close();
        }
    }
}
