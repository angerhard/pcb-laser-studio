package de.andreasgerhard.pcblaserstudio.view.dialogs;

import de.andreasgerhard.pcblaserstudio.model.ConnectionSettings;
import de.andreasgerhard.pcblaserstudio.service.PreferencesService;
import de.andreasgerhard.pcblaserstudio.service.serial.GrblConnection;
import de.andreasgerhard.pcblaserstudio.service.serial.GrblStatus;
import de.andreasgerhard.pcblaserstudio.service.serial.LaserConnection;
import de.andreasgerhard.pcblaserstudio.service.serial.LaserPortScanner;
import de.andreasgerhard.pcblaserstudio.service.serial.LaserStreamer;
import de.andreasgerhard.pcblaserstudio.service.serial.SerialPortInfo;
import de.andreasgerhard.pcblaserstudio.service.serial.SimulatedLaserConnection;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Locale;

/**
 * Non-modal dialog for connecting to a laser cutter and streaming GCODE.
 *
 * <p>Shows the connection controls, live machine feedback, a raw console for
 * troubleshooting, and a running job with percentage progress plus
 * pause/resume/stop.</p>
 */
public class LaserControlDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(LaserControlDialog.class);
    private static final int MAX_CONSOLE_LINES = 2000;

    private final PreferencesService preferences;
    private final ConnectionSettings settings;

    // Connection controls
    private final JComboBox<SerialPortInfo> portCombo = new JComboBox<>();
    private final JComboBox<Integer> baudCombo = new JComboBox<>(
            new Integer[]{9600, 19200, 38400, 57600, 115200, 230400, 250000, 500000, 921600});
    private final JCheckBox dtrCheck = new JCheckBox("DTR");
    private final JCheckBox simCheck = new JCheckBox("Simulator");
    private final JButton connectBtn = DarkTheme.createButton("Connect", "Open the serial connection");
    private final JLabel connStatus = DarkTheme.createDimLabel("Not connected");

    // Job controls
    private final JLabel jobLabel = DarkTheme.createLabel("-");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel progressLabel = DarkTheme.createDimLabel("0 / 0 lines (0%)");
    private final JLabel elapsedLabel = DarkTheme.createDimLabel("Elapsed: 0:00");
    private final JLabel etaLabel = DarkTheme.createDimLabel("ETA: -");
    private final JLabel stateValue = DarkTheme.createLabel("-");
    private final JLabel posValue = DarkTheme.createLabel("-");
    private final JLabel feedValue = DarkTheme.createLabel("-");
    private final JLabel laserValue = DarkTheme.createLabel("-");
    private final JButton startBtn = DarkTheme.createAccentButton("Start", "Send the GCODE to the laser");
    private final JButton pauseBtn = DarkTheme.createButton("Pause", "Feed hold / resume");
    private final JButton stopBtn = DarkTheme.createButton("Stop", "Abort the job (laser off + reset)");

    // Console
    private final JTextArea consoleArea = new JTextArea();
    private final JTextField consoleInput = new JTextField();
    private final JButton consoleSend = DarkTheme.createButton("Send", "Send a raw command");

    private LaserConnection connection;
    private LaserStreamer streamer;

    private String jobName = "GCODE";
    private String jobGcode;
    private long estimatedSeconds;

    private final Timer elapsedTimer;
    private long jobStartMillis;
    private int lastAcked;
    private int lastTotal;

    public LaserControlDialog(JFrame owner, ConnectionSettings settings, PreferencesService preferences) {
        super(owner, "Send to Laser", false);
        this.settings = settings;
        this.preferences = preferences;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(560, 620));
        getContentPane().setBackground(DarkTheme.BG_DARK);
        getContentPane().setLayout(new MigLayout("fill, insets 8, gapy 6", "[grow]", "[][][grow][][pref]"));

        buildConnectionSection();
        buildJobSection();
        buildConsoleSection();

        elapsedTimer = new Timer(1000, e -> updateElapsed());
        elapsedTimer.setRepeats(true);

        applySettingsToUi();
        refreshPorts();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseRequested();
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    // =================== UI construction ===================

    private void buildConnectionSection() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 6", "[grow][pref][pref]", ""));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(DarkTheme.createSectionBorder("Connection"));

        panel.add(DarkTheme.createDimLabel("Port:"), "split 3");
        portCombo.setFont(DarkTheme.FONT_SMALL);
        portCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof SerialPortInfo info ? info.display()
                        : value == null ? "(no ports found)" : value.toString());
                return this;
            }
        });
        panel.add(portCombo, "growx, wmin 180");
        JButton refreshBtn = DarkTheme.createButton("Refresh", "Rescan serial ports");
        refreshBtn.addActionListener(e -> refreshPorts());
        panel.add(refreshBtn, "wrap");

        panel.add(DarkTheme.createDimLabel("Baud:"), "split 5");
        baudCombo.setFont(DarkTheme.FONT_SMALL);
        panel.add(baudCombo, "w 90!");
        dtrCheck.setFont(DarkTheme.FONT_SMALL);
        dtrCheck.setForeground(DarkTheme.TEXT_DIM);
        dtrCheck.setBackground(DarkTheme.BG_DARK);
        panel.add(dtrCheck);
        simCheck.setFont(DarkTheme.FONT_SMALL);
        simCheck.setForeground(DarkTheme.TEXT_DIM);
        simCheck.setBackground(DarkTheme.BG_DARK);
        simCheck.setToolTipText("Use the built-in simulator (no hardware)");
        panel.add(simCheck);
        connectBtn.addActionListener(e -> toggleConnection());
        panel.add(connectBtn, "w 110!");
        panel.add(connStatus, "wrap, gapleft 8");

        simCheck.addActionListener(e -> {
            boolean sim = simCheck.isSelected();
            portCombo.setEnabled(!sim);
            baudCombo.setEnabled(!sim);
            dtrCheck.setEnabled(!sim);
        });

        add(panel, "growx, wrap");
    }

    private void buildJobSection() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 6", "[grow]", ""));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(DarkTheme.createSectionBorder("Job"));

        panel.add(DarkTheme.createDimLabel("Layer / file:"), "split 2");
        panel.add(jobLabel, "wrap");

        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setFont(DarkTheme.FONT_SMALL);
        panel.add(progressBar, "growx, h 20!, wrap");
        panel.add(progressLabel, "growx, wrap");

        JPanel stats = new JPanel(new MigLayout("fillx, insets 3 6 3 6, gapx 8", "[pref][grow][pref][grow]", ""));
        stats.setBackground(DarkTheme.BG_DARKEST);
        stats.setBorder(BorderFactory.createLineBorder(DarkTheme.BG_MEDIUM, 1));
        stats.add(DarkTheme.createDimLabel("State:"));
        stats.add(stateValue);
        stats.add(DarkTheme.createDimLabel("Position:"));
        stats.add(posValue, "wrap");
        stats.add(DarkTheme.createDimLabel("Feed:"));
        stats.add(feedValue);
        stats.add(DarkTheme.createDimLabel("Laser:"));
        stats.add(laserValue, "wrap");
        panel.add(stats, "growx, wrap");

        JPanel timing = new JPanel(new MigLayout("insets 0, fillx, gapx 12", "[grow][grow]", ""));
        timing.setBackground(DarkTheme.BG_DARK);
        timing.add(elapsedLabel);
        timing.add(etaLabel, "wrap");
        panel.add(timing, "growx, wrap");

        pauseBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        pauseBtn.addActionListener(e -> togglePause());
        stopBtn.addActionListener(e -> stopJob());
        startBtn.addActionListener(e -> startJob());

        panel.add(startBtn, "split 3, growx, h 24!");
        panel.add(pauseBtn, "growx, h 24!");
        panel.add(stopBtn, "growx, h 24!, wrap");

        add(panel, "growx, wrap");
    }

    private void buildConsoleSection() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 6", "[grow]", "[grow][pref]"));
        panel.setBackground(DarkTheme.BG_DARK);
        panel.setBorder(DarkTheme.createSectionBorder("Controller Console"));

        consoleArea.setEditable(false);
        consoleArea.setFont(DarkTheme.FONT_MONO);
        consoleArea.setBackground(DarkTheme.BG_DARKEST);
        consoleArea.setForeground(DarkTheme.TEXT_PRIMARY);
        consoleArea.setLineWrap(false);
        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setPreferredSize(new Dimension(100, 140));
        panel.add(scroll, "grow, wrap");

        consoleInput.setFont(DarkTheme.FONT_MONO);
        consoleInput.setBackground(DarkTheme.BG_MEDIUM);
        consoleInput.setForeground(DarkTheme.TEXT_PRIMARY);
        consoleInput.setCaretColor(DarkTheme.TEXT_PRIMARY);
        consoleInput.setToolTipText("Send a raw command, e.g. $I, $$ or ?");
        consoleInput.addActionListener(e -> sendRawCommand());
        consoleSend.addActionListener(e -> sendRawCommand());
        panel.add(consoleInput, "split 2, growx, h 24!");
        panel.add(consoleSend, "w 70!, h 24!");

        add(panel, "grow, push, wrap");
    }

    // =================== Public API ===================

    /** Loads the job to be sent. The user still has to press Start. */
    public void setJob(String name, String gcode, long estimatedSeconds) {
        this.jobName = name != null ? name : "GCODE";
        this.jobGcode = gcode;
        this.estimatedSeconds = Math.max(0, estimatedSeconds);
        this.lastAcked = 0;
        this.lastTotal = 0;

        int lines = LaserStreamer.parseCommands(gcode).size();
        jobLabel.setText(String.format("%s  (%,d lines)", this.jobName, lines));
        progressBar.setValue(0);
        progressBar.setString("0%");
        progressLabel.setText(String.format(Locale.ENGLISH, "0 / %,d lines (0%%)", lines));
        etaLabel.setText(estimatedSeconds > 0
                ? "ETA: " + formatDuration(estimatedSeconds) : "ETA: -");
        startBtn.setEnabled(true);
    }

    // =================== Connection ===================

    private void refreshPorts() {
        portCombo.removeAllItems();
        List<SerialPortInfo> ports = LaserPortScanner.scan();
        for (SerialPortInfo info : ports) portCombo.addItem(info);
        if (ports.isEmpty()) {
            connStatus.setText("No serial ports found");
        } else {
            connStatus.setText(ports.size() + " port(s) found");
        }
        selectSavedPort();
    }

    private void selectSavedPort() {
        String saved = settings.getPortName();
        if (saved == null) return;
        for (int i = 0; i < portCombo.getItemCount(); i++) {
            if (saved.equals(portCombo.getItemAt(i).systemPortName())) {
                portCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void applySettingsToUi() {
        baudCombo.setSelectedItem(settings.getBaudRate());
        if (baudCombo.getSelectedItem() == null) baudCombo.setSelectedItem(115200);
        dtrCheck.setSelected(settings.isDtr());
        simCheck.setSelected(settings.isSimulator());
        portCombo.setEnabled(!settings.isSimulator());
        baudCombo.setEnabled(!settings.isSimulator());
        dtrCheck.setEnabled(!settings.isSimulator());
    }

    private void readSettingsFromUi() {
        if (simCheck.isSelected()) {
            settings.setSimulator(true);
        } else {
            settings.setSimulator(false);
            SerialPortInfo info = (SerialPortInfo) portCombo.getSelectedItem();
            if (info != null) settings.setPortName(info.systemPortName());
        }
        Object baud = baudCombo.getSelectedItem();
        if (baud instanceof Integer b) settings.setBaudRate(b);
        settings.setDtr(dtrCheck.isSelected());
        preferences.save(settings);
    }

    private void toggleConnection() {
        if (connection != null && connection.isOpen()) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        readSettingsFromUi();
        if (!settings.isSimulator() && (settings.getPortName() == null || settings.getPortName().isBlank())) {
            JOptionPane.showMessageDialog(this, "Please select a serial port first.",
                    "No Port", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            connection = settings.isSimulator()
                    ? new SimulatedLaserConnection()
                    : new GrblConnection(settings);
            connection.addListener(new UiConnectionListener());
            connection.open();

            connectBtn.setText("Disconnect");
            connStatus.setText("Connected: " + connection.getPortDescription());
            appendConsole("--- connected: " + connection.getPortDescription() + " ---");
            startBtn.setEnabled(jobGcode != null);
        } catch (Exception ex) {
            log.error("Connect failed", ex);
            connection = null;
            connStatus.setText("Connect failed");
            JOptionPane.showMessageDialog(this,
                    "Could not connect:\n" + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect() {
        if (streamer != null && !isStreamingDone()) {
            streamer.stop();
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        connectBtn.setText("Connect");
        connStatus.setText("Not connected");
        startBtn.setEnabled(false);
        setPauseEnabled(false);
        setStopEnabled(false);
    }

    // =================== Streaming ===================

    private void startJob() {
        if (jobGcode == null || jobGcode.isBlank()) {
            JOptionPane.showMessageDialog(this, "No GCODE to send.", "Nothing to Send",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (connection == null || !connection.isOpen()) {
            JOptionPane.showMessageDialog(this, "Connect to the laser first.", "Not Connected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!confirmSend()) return;

        streamer = new LaserStreamer(connection, jobGcode, new UiStreamListener());
        lastAcked = 0;
        lastTotal = streamer.getTotalLines();
        progressBar.setValue(0);
        progressBar.setString("0%");
        progressLabel.setText(String.format(Locale.ENGLISH, "0 / %,d lines (0%%)", lastTotal));
        jobStartMillis = System.currentTimeMillis();
        elapsedTimer.start();
        startBtn.setEnabled(false);
        pauseBtn.setText("Pause");
        setPauseEnabled(true);
        setStopEnabled(true);
        appendConsole("--- sending " + jobName + " (" + lastTotal + " lines) ---");
        streamer.start();
    }

    private boolean confirmSend() {
        String target = connection.getPortDescription();
        int lines = streamerTotal();
        int r = JOptionPane.showConfirmDialog(this,
                String.format("Send \"%s\" (%d lines) to:\n%s ?", jobName, lines, target),
                "Confirm Send", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return r == JOptionPane.OK_OPTION;
    }

    private int streamerTotal() {
        return jobGcode == null ? 0 : LaserStreamer.parseCommands(jobGcode).size();
    }

    private void togglePause() {
        if (streamer == null) return;
        if (streamer.getState() == LaserStreamer.State.PAUSED) {
            streamer.resume();
            pauseBtn.setText("Pause");
        } else if (streamer.getState() == LaserStreamer.State.RUNNING) {
            streamer.pause();
            pauseBtn.setText("Resume");
        }
    }

    private void stopJob() {
        if (streamer == null || isStreamingDone()) return;
        int r = JOptionPane.showConfirmDialog(this,
                "Stop the current job and turn the laser off?",
                "Stop Job", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            streamer.stop();
            appendConsole("--- stop requested ---");
        }
    }

    private boolean isStreamingDone() {
        if (streamer == null) return true;
        LaserStreamer.State s = streamer.getState();
        return s == LaserStreamer.State.FINISHED
                || s == LaserStreamer.State.CANCELLED
                || s == LaserStreamer.State.ERROR;
    }

    private void sendRawCommand() {
        String cmd = consoleInput.getText();
        if (cmd == null || cmd.isBlank()) return;
        if (connection == null || !connection.isOpen()) {
            appendConsole("! not connected");
            return;
        }
        appendConsole("> " + cmd);
        connection.sendLine(cmd.trim());
        consoleInput.setText("");
    }

    // =================== Listeners ===================

    private final class UiConnectionListener implements LaserConnection.Listener {
        @Override
        public void onLine(String line) {
            onEdt(() -> appendConsole(line));
        }

        @Override
        public void onStatus(GrblStatus status) {
            onEdt(() -> {
                stateValue.setText(status.state());
                posValue.setText(status.positionText());
                feedValue.setText(status.feedText());
                laserValue.setText(status.laserText());
            });
        }

        @Override
        public void onDisconnected(String reason) {
            onEdt(() -> {
                connStatus.setText("Disconnected: " + (reason != null ? reason : ""));
                connectBtn.setText("Connect");
                startBtn.setEnabled(false);
                setPauseEnabled(false);
                setStopEnabled(false);
                appendConsole("--- disconnected ---");
            });
        }
    }

    private final class UiStreamListener implements LaserStreamer.Listener {
        @Override
        public void onProgress(int acked, int total, int percent) {
            onEdt(() -> {
                lastAcked = acked;
                lastTotal = total;
                progressBar.setValue(percent);
                progressBar.setString(percent + "%");
                progressLabel.setText(String.format(Locale.ENGLISH,
                        "%,d / %,d lines (%d%%)", acked, total, percent));
                updateEta(percent);
            });
        }

        @Override
        public void onStateChanged(LaserStreamer.State state) {
            onEdt(() -> stateValue.setText(state.name()));
        }

        @Override
        public void onFinished(LaserStreamer.Result result) {
            onEdt(() -> {
                elapsedTimer.stop();
                updateElapsed();
                setPauseEnabled(false);
                setStopEnabled(false);
                startBtn.setEnabled(connection != null && connection.isOpen());
                pauseBtn.setText("Pause");
                String msg = switch (result.finalState()) {
                    case FINISHED -> String.format(Locale.ENGLISH,
                            "Job finished: %,d lines.", result.ackedLines());
                    case CANCELLED -> "Job cancelled.";
                    case ERROR -> "Job failed: " + result.errorMessage();
                    default -> result.finalState().name();
                };
                appendConsole("--- " + msg + " ---");
                if (result.finalState() == LaserStreamer.State.ERROR) {
                    JOptionPane.showMessageDialog(LaserControlDialog.this,
                            msg, "Job Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    // =================== Helpers ===================

    private void updateElapsed() {
        long elapsed = (System.currentTimeMillis() - jobStartMillis) / 1000;
        elapsedLabel.setText("Elapsed: " + formatDuration(elapsed));
    }

    private void updateEta(int percent) {
        long remaining = -1;
        if (estimatedSeconds > 0) {
            remaining = Math.round(estimatedSeconds * (100 - percent) / 100.0);
        } else if (lastAcked > 0 && lastTotal > 0) {
            long elapsed = (System.currentTimeMillis() - jobStartMillis) / 1000;
            remaining = elapsed * (lastTotal - lastAcked) / lastAcked;
        }
        etaLabel.setText(remaining >= 0 ? "ETA: " + formatDuration(remaining) : "ETA: -");
    }

    private static String formatDuration(long seconds) {
        if (seconds < 0) return "-";
        return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void appendConsole(String line) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(line + "\n");
            int lines = consoleArea.getLineCount();
            if (lines > MAX_CONSOLE_LINES) {
                try {
                    int cut = consoleArea.getLineEndOffset(lines - MAX_CONSOLE_LINES);
                    consoleArea.replaceRange("", 0, cut);
                } catch (Exception ignored) {
                    // ignore
                }
            }
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private void setPauseEnabled(boolean enabled) {
        pauseBtn.setEnabled(enabled);
    }

    private void setStopEnabled(boolean enabled) {
        stopBtn.setEnabled(enabled);
    }

    private static void onEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    private void onCloseRequested() {
        if (streamer != null && !isStreamingDone()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "A job is running. Stop it and close?",
                    "Job Running", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
            streamer.stop();
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        elapsedTimer.stop();
        // Remember the last-used connection settings for next time.
        try {
            readSettingsFromUi();
        } catch (Exception ex) {
            log.warn("Could not save connection settings: {}", ex.getMessage());
        }
        dispose();
    }
}
