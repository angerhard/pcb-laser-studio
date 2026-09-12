package de.andreasgerhard.pcblaserstudio.service.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import de.andreasgerhard.pcblaserstudio.model.ConnectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link LaserConnection} implementation for GRBL-compatible controllers over
 * a USB serial port (e.g. Creality Falcon A1).
 *
 * <p>Reads are event-driven and line-buffered; a background scheduler polls the
 * controller with {@code ?} so the UI receives regular status feedback.</p>
 */
public class GrblConnection implements LaserConnection {

    private static final Logger log = LoggerFactory.getLogger(GrblConnection.class);
    private static final long STATUS_POLL_MS = 400;

    private final ConnectionSettings settings;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Object writeLock = new Object();
    private final StringBuilder lineBuffer = new StringBuilder();

    private volatile SerialPort port;
    private volatile ScheduledExecutorService statusPoller;
    private volatile boolean closing;

    public GrblConnection(ConnectionSettings settings) {
        this.settings = new ConnectionSettings(settings);
    }

    @Override
    public void open() throws IOException {
        if (isOpen()) return;
        String portName = settings.getPortName();
        if (portName == null || portName.isBlank()) {
            throw new IOException("No serial port selected.");
        }
        closing = false;
        SerialPort p = SerialPort.getCommPort(portName);
        p.setBaudRate(settings.getBaudRate());
        p.setNumDataBits(8);
        p.setNumStopBits(SerialPort.ONE_STOP_BIT);
        p.setParity(SerialPort.NO_PARITY);
        p.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!p.openPort()) {
            throw new IOException("Could not open " + portName
                    + " (is it in use by another program, or missing permissions?)");
        }

        // DTR is applied after opening so it does not interfere with the open sequence.
        try {
            if (settings.isDtr()) p.setDTR(); else p.clearDTR();
            p.clearRTS();
        } catch (Exception ex) {
            log.warn("Could not set DTR/RTS on {}: {}", portName, ex.getMessage());
        }

        this.port = p;
        p.addDataListener(new DataListener());

        statusPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grbl-status-poller");
            t.setDaemon(true);
            return t;
        });
        statusPoller.scheduleAtFixedRate(this::requestStatus,
                STATUS_POLL_MS, STATUS_POLL_MS, TimeUnit.MILLISECONDS);

        log.info("Opened GRBL connection on {} @ {} baud (DTR={})",
                portName, settings.getBaudRate(), settings.isDtr());
    }

    @Override
    public void close() {
        closing = true;
        ScheduledExecutorService poller = statusPoller;
        statusPoller = null;
        if (poller != null) poller.shutdownNow();

        SerialPort p = port;
        port = null;
        if (p != null) {
            try {
                p.removeDataListener();
            } catch (Exception ignored) {
                // ignore
            }
            try {
                if (p.isOpen()) p.closePort();
            } catch (Exception ex) {
                log.warn("Error closing port: {}", ex.getMessage());
            }
        }
        lineBuffer.setLength(0);
        log.info("GRBL connection closed");
    }

    @Override
    public boolean isOpen() {
        SerialPort p = port;
        return p != null && p.isOpen();
    }

    @Override
    public String getPortDescription() {
        SerialPort p = port;
        if (p != null) return p.getSystemPortName() + " @ " + settings.getBaudRate();
        return settings.getPortName() != null ? settings.getPortName() : "(none)";
    }

    @Override
    public void sendLine(String line) {
        if (line == null) return;
        write((line + "\n").getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public void sendRealtime(byte command) {
        write(new byte[]{command});
    }

    private void write(byte[] data) {
        SerialPort p = port;
        if (p == null || !p.isOpen()) return;
        synchronized (writeLock) {
            try {
                p.writeBytes(data, data.length);
            } catch (Exception ex) {
                fireError("Write failed: " + ex.getMessage());
            }
        }
    }

    private void processByte(byte b) {
        char c = (char) (b & 0xFF);
        if (c == '\n' || c == '\r') {
            if (lineBuffer.length() > 0) {
                String line = lineBuffer.toString();
                lineBuffer.setLength(0);
                dispatchLine(line);
            }
            return;
        }
        if (c == 0x18) { // Ctrl-X echoed by some firmwares
            return;
        }
        lineBuffer.append(c);
        if (lineBuffer.length() > 4096) {
            // Protect against a controller that never sends a newline.
            lineBuffer.setLength(0);
        }
    }

    private void dispatchLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return;

        GrblStatus status = GrblStatus.parse(trimmed);
        for (Listener l : listeners) {
            try {
                l.onLine(trimmed);
                if (status != null) l.onStatus(status);
            } catch (Exception ex) {
                log.warn("Listener error: {}", ex.getMessage());
            }
        }
    }

    private void handleDisconnect(String reason) {
        if (closing) return;
        log.warn("GRBL port disconnected: {}", reason);
        close();
        for (Listener l : listeners) {
            try {
                l.onDisconnected(reason);
            } catch (Exception ex) {
                log.warn("Listener error: {}", ex.getMessage());
            }
        }
    }

    private void fireError(String message) {
        log.warn(message);
        for (Listener l : listeners) {
            try {
                l.onError(message);
            } catch (Exception ex) {
                log.warn("Listener error: {}", ex.getMessage());
            }
        }
    }

    @Override
    public void addListener(Listener listener) {
        if (listener != null) listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private final class DataListener implements SerialPortDataListener {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
                    | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                handleDisconnect("USB port disconnected");
                return;
            }
            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;
            SerialPort p = port;
            if (p == null || !p.isOpen()) return;
            int available = p.bytesAvailable();
            if (available <= 0) return;
            byte[] buffer = new byte[available];
            int read = p.readBytes(buffer, buffer.length);
            for (int i = 0; i < read; i++) processByte(buffer[i]);
        }
    }
}
