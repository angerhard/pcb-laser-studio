package de.andreasgerhard.pcblaserstudio.service.serial;

import java.io.IOException;

/**
 * Abstraction over a connection to a laser cutter controller.
 *
 * <p>Implementations exist for real GRBL controllers over USB serial
 * ({@link GrblConnection}) and for a hardware-free simulator
 * ({@link SimulatedLaserConnection}). Additional machine families can be
 * supported by adding new implementations without changing the UI or the
 * streaming logic.</p>
 */
public interface LaserConnection {

    /** Receives asynchronous events from the controller. */
    interface Listener {
        /** A complete line received from the controller (already trimmed). */
        default void onLine(String line) {}
        /** A parsed GRBL status report (from a {@code <?>} query). */
        default void onStatus(GrblStatus status) {}
        /** The port was closed or lost. {@code reason} may be null. */
        default void onDisconnected(String reason) {}
        /** A non-fatal I/O or protocol error occurred. */
        default void onError(String message) {}
    }

    /** Opens the connection. @throws IOException if the port cannot be opened. */
    void open() throws IOException;

    /** Closes the connection and releases resources. Safe to call repeatedly. */
    void close();

    boolean isOpen();

    /** Human-readable description of the connected port (for the UI). */
    String getPortDescription();

    /** Sends a normal command line (terminated with {@code \n}). */
    void sendLine(String line);

    /** Sends a GRBL real-time command byte (e.g. {@code ?}, {@code !}, {@code ~}, {@code 0x18}). */
    void sendRealtime(byte command);

    /** Requests a status report ({@code ?}). */
    default void requestStatus() { sendRealtime((byte) '?'); }

    /** Feed hold ({@code !}) – pauses motion. */
    default void feedHold() { sendRealtime((byte) '!'); }

    /** Cycle start / resume ({@code ~}). */
    default void resume() { sendRealtime((byte) '~'); }

    /** Soft reset ({@code Ctrl-X}) – aborts the current job. */
    default void softReset() { sendRealtime((byte) 0x18); }

    void addListener(Listener listener);
    void removeListener(Listener listener);
}
