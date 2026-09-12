package de.andreasgerhard.pcblaserstudio.service.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hardware-free {@link LaserConnection} that mimics a GRBL controller.
 *
 * <p>Every command line is answered asynchronously with {@code ok} after a
 * short delay, and {@code ?} queries return a plausible status report. This
 * lets the connection dialog, progress tracking, pause/resume and stop logic
 * be exercised without a real laser.</p>
 */
public class SimulatedLaserConnection implements LaserConnection {

    private static final Logger log = LoggerFactory.getLogger(SimulatedLaserConnection.class);
    private static final Pattern X_COORD = Pattern.compile("X(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern Y_COORD = Pattern.compile("Y(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern S_VALUE = Pattern.compile("S(\\d+)");

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean open;
    private volatile String state = "Idle";
    private volatile double x = 0, y = 0;
    private volatile double feed = 0, spindle = 0;

    private ScheduledExecutorService executor;

    @Override
    public void open() throws IOException {
        if (open) return;
        open = true;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "laser-simulator");
            t.setDaemon(true);
            return t;
        });
        log.info("Simulated laser connection opened");
    }

    @Override
    public void close() {
        open = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        for (Listener l : listeners) l.onDisconnected("Simulator closed");
        log.info("Simulated laser connection closed");
    }

    @Override
    public boolean isOpen() { return open; }

    @Override
    public String getPortDescription() { return "Simulator (no hardware)"; }

    @Override
    public void sendLine(String line) {
        if (!open || line == null) return;
        ScheduledExecutorService ex = executor;
        if (ex == null) return;
        ex.schedule(() -> {
            if (!open) return;
            handleCommand(line.trim());
            fireLine("ok");
        }, 1, TimeUnit.MILLISECONDS);
    }

    @Override
    public void sendRealtime(byte command) {
        if (!open) return;
        ScheduledExecutorService ex = executor;
        if (ex == null) return;
        ex.execute(() -> {
            switch (command) {
                case '?' -> fireStatus();
                case '!' -> { state = "Hold"; fireStatus(); }
                case '~' -> { if ("Hold".equals(state)) state = "Run"; fireStatus(); }
                case 0x18 -> { state = "Idle"; spindle = 0; fireStatus(); }
                default -> { /* ignore */ }
            }
        });
    }

    private void handleCommand(String line) {
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("(")) return;

        boolean motion = line.startsWith("G0") || line.startsWith("G1")
                || line.startsWith("G00") || line.startsWith("G01");
        Matcher mx = X_COORD.matcher(line);
        Matcher my = Y_COORD.matcher(line);
        if (mx.find()) x = parse(mx.group(1), x);
        if (my.find()) y = parse(my.group(1), y);
        Matcher ms = S_VALUE.matcher(line);
        if (ms.find()) spindle = parse(ms.group(1), spindle);
        if (line.startsWith("M5") || line.startsWith("S0")) spindle = 0;

        if (motion) {
            state = "Run";
            feed = 3000;
            // Fall back to Idle shortly after the last motion command.
            ScheduledExecutorService ex = executor;
            if (ex != null) {
                ex.schedule(() -> {
                    if ("Run".equals(state)) state = "Idle";
                }, 250, TimeUnit.MILLISECONDS);
            }
        }
    }

    private static double parse(String s, double fallback) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return fallback; }
    }

    private void fireStatus() {
        String raw = String.format(Locale.ENGLISH,
                "<%s|MPos:%.3f,%.3f,0.000|FS:%.0f,%.0f>",
                state, x, y, feed, spindle);
        GrblStatus status = GrblStatus.parse(raw);
        for (Listener l : listeners) {
            l.onLine(raw);
            if (status != null) l.onStatus(status);
        }
    }

    private void fireLine(String line) {
        for (Listener l : listeners) l.onLine(line);
    }

    @Override
    public void addListener(Listener listener) {
        if (listener != null) listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
