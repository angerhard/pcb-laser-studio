package de.andreasgerhard.pcblaserstudio.service.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streams a GCODE program to a {@link LaserConnection} using the simple
 * "send one line, wait for {@code ok}" flow-control strategy.
 *
 * <p>Tracks acknowledged lines to report job progress, and supports
 * pause ({@code !}), resume ({@code ~}) and stop (feed-hold + soft reset).</p>
 */
public class LaserStreamer {

    private static final Logger log = LoggerFactory.getLogger(LaserStreamer.class);
    private static final long RESPONSE_TIMEOUT_MS = 30_000;
    private static final long POLL_SLICE_MS = 100;

    public enum State { IDLE, RUNNING, PAUSED, CANCELLED, FINISHED, ERROR }

    /** Result of a completed (or aborted) streaming job. */
    public record Result(int ackedLines, int totalLines, State finalState, String errorMessage) {
        public boolean isSuccess() { return finalState == State.FINISHED; }
    }

    /** Receives progress and feedback during streaming. */
    public interface Listener {
        default void onProgress(int ackedLines, int totalLines, int percent) {}
        default void onStateChanged(State state) {}
        default void onMachineLine(String line) {}
        default void onStatus(GrblStatus status) {}
        default void onFinished(Result result) {}
    }

    private final LaserConnection connection;
    private final List<String> commands;
    private final Listener listener;

    private final BlockingQueue<String> responses = new LinkedBlockingQueue<>();
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private volatile State state = State.IDLE;
    private volatile boolean paused;
    private volatile boolean cancelled;
    private volatile int ackedCount;
    private volatile Thread worker;
    private volatile LaserConnection.Listener connectionListener;

    public LaserStreamer(LaserConnection connection, String gcode, Listener listener) {
        this.connection = connection;
        this.listener = listener != null ? listener : new Listener() {};
        this.commands = parseCommands(gcode);
    }

    public int getTotalLines() { return commands.size(); }
    public State getState() { return state; }

    /** Starts streaming on a background daemon thread. */
    public void start() {
        if (state != State.IDLE) return;
        if (!connection.isOpen()) {
            fail("Not connected to a laser.");
            return;
        }
        connectionListener = new LaserConnection.Listener() {
            @Override
            public void onLine(String line) {
                listener.onMachineLine(line);
                if (isTerminal(line)) responses.offer(line);
            }

            @Override
            public void onStatus(GrblStatus status) {
                listener.onStatus(status);
            }

            @Override
            public void onDisconnected(String reason) {
                abort("Disconnected: " + (reason != null ? reason : "port closed"));
            }

            @Override
            public void onError(String message) {
                listener.onMachineLine("! " + message);
            }
        };
        connection.addListener(connectionListener);

        worker = new Thread(this::streamLoop, "laser-streamer");
        worker.setDaemon(true);
        setState(State.RUNNING);
        worker.start();
    }

    /** Pauses motion using GRBL feed hold. */
    public void pause() {
        if (state != State.RUNNING) return;
        paused = true;
        connection.feedHold();
        setState(State.PAUSED);
    }

    /** Resumes after a pause. */
    public void resume() {
        if (state != State.PAUSED) return;
        paused = false;
        connection.resume();
        setState(State.RUNNING);
    }

    /** Stops the job: feed hold, laser off, soft reset. */
    public void stop() {
        if (isTerminalState(state)) return;
        cancelled = true;
        paused = false;
        try {
            connection.feedHold();
            connection.sendLine("M5");
            connection.softReset();
        } catch (Exception ex) {
            log.warn("Error while stopping: {}", ex.getMessage());
        }
        responses.clear();
    }

    // =================== Internal ===================

    private void streamLoop() {
        int acked = 0;
        int total = commands.size();
        try {
            for (String cmd : commands) {
                awaitResumed();
                if (cancelled) break;
                connection.sendLine(cmd);
                String response = awaitResponse();
                if (response == null) {
                    if (cancelled) break;
                    fail("Timeout waiting for controller response.");
                    return;
                }
                if (response.startsWith("error") || response.startsWith("ALARM")) {
                    fail("Controller reported: " + response);
                    return;
                }
                acked++;
                ackedCount = acked;
                int percent = total == 0 ? 100 : (int) (acked * 100L / total);
                listener.onProgress(acked, total, percent);
            }

            if (cancelled) {
                finish(State.CANCELLED, acked, total, "Job stopped by user.");
            } else {
                finish(State.FINISHED, acked, total, null);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            finish(State.CANCELLED, acked, total, "Interrupted.");
        } catch (Exception ex) {
            log.error("Streaming error", ex);
            fail("Streaming error: " + ex.getMessage());
        }
    }

    private void awaitResumed() throws InterruptedException {
        while (paused && !cancelled) {
            Thread.sleep(50);
        }
    }

    private String awaitResponse() throws InterruptedException {
        long deadline = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (cancelled) return null;
            String response = responses.poll(POLL_SLICE_MS, TimeUnit.MILLISECONDS);
            if (response != null) return response;
        }
        return null;
    }

    private static boolean isTerminal(String line) {
        return line.equals("ok")
                || line.startsWith("error")
                || line.startsWith("ALARM");
    }

    private void abort(String message) {
        cancelled = true;
        paused = false;
        responses.clear();
        fail(message);
    }

    private void fail(String message) {
        if (isTerminalState(state)) return;
        detachListener();
        setState(State.ERROR);
        completeOnce(new Result(currentAcked(), commands.size(), State.ERROR, message));
    }

    private void finish(State finalState, int acked, int total, String message) {
        detachListener();
        setState(finalState);
        completeOnce(new Result(acked, total, finalState, message));
    }

    private void completeOnce(Result result) {
        if (finished.compareAndSet(false, true)) {
            listener.onFinished(result);
        }
    }

    private int currentAcked() {
        return ackedCount;
    }

    private void detachListener() {
        LaserConnection.Listener l = connectionListener;
        if (l != null) {
            connection.removeListener(l);
            connectionListener = null;
        }
    }

    private void setState(State newState) {
        if (state == newState) return;
        state = newState;
        listener.onStateChanged(newState);
    }

    private static boolean isTerminalState(State s) {
        return s == State.FINISHED || s == State.CANCELLED || s == State.ERROR;
    }

    /**
     * Splits GCODE into commands, dropping blank lines and pure comment lines.
     * Inline comments (after {@code ;}) are preserved because GRBL ignores them.
     */
    public static List<String> parseCommands(String gcode) {
        List<String> out = new ArrayList<>();
        if (gcode == null) return out;
        for (String raw : gcode.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith(";") || line.startsWith("(")) continue;
            out.add(line);
        }
        return out;
    }
}
