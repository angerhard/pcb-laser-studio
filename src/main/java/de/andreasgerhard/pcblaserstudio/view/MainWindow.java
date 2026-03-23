package de.andreasgerhard.pcblaserstudio.view;

import de.andreasgerhard.pcblaserstudio.model.Layer;
import de.andreasgerhard.pcblaserstudio.model.Project;
import de.andreasgerhard.pcblaserstudio.view.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main application window: menu bar, tabbed pane with 4 layers, status bar.
 */
public class MainWindow extends JFrame {

    private final JTabbedPane tabbedPane;
    private final JLabel      statusLabel;
    private final JLabel      statusRight;
    private final List<LayerTab>          layerTabs        = new ArrayList<>();
    private final List<LayerTabComponent> tabComponents    = new ArrayList<>();

    // Menu actions (wired by controller)
    public JMenuItem menuNew, menuOpen, menuSave, menuSaveAs, menuExit;
    public JMenuItem menuOpenSvg, menuClearSvg;
    public JMenuItem menuGenAll, menuGenActive, menuExportGcode, menuExportProject;
    public JMenuItem menuAbout;

    private Consumer<Integer> onTabChanged;
    private Consumer<Void>    onCloseRequested;

    public MainWindow() {
        super("PCB Laser Studio");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setPreferredSize(new Dimension(1280, 800));

        buildMenuBar();

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(DarkTheme.BG_DARKEST);
        tabbedPane.setForeground(DarkTheme.TEXT_PRIMARY);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(DarkTheme.BG_DARKEST);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DarkTheme.BG_MEDIUM),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(DarkTheme.FONT_SMALL);
        statusLabel.setForeground(DarkTheme.TEXT_DIM);
        statusRight = new JLabel("");
        statusRight.setFont(DarkTheme.FONT_SMALL);
        statusRight.setForeground(DarkTheme.TEXT_DIM);
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(statusRight, BorderLayout.EAST);

        getContentPane().setBackground(DarkTheme.BG_DARKEST);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(statusBar,  BorderLayout.SOUTH);

        tabbedPane.addChangeListener(e -> {
            if (onTabChanged != null) onTabChanged.accept(tabbedPane.getSelectedIndex());
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onCloseRequested != null) onCloseRequested.accept(null);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    // =================== Tab management ===================

    public void initTabs(Project project) {
        tabbedPane.removeAll();
        layerTabs.clear();
        tabComponents.clear();

        for (Layer layer : project.getLayers()) {
            LayerTab tab = new LayerTab(layer);
            LayerTabComponent header = new LayerTabComponent(layer);

            layerTabs.add(tab);
            tabComponents.add(header);

            tabbedPane.addTab(null, tab);
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, header);
        }

        // Apply dividers after showing
        SwingUtilities.invokeLater(() -> layerTabs.forEach(LayerTab::applyDivider));
    }

    public LayerTab getActiveTab() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx < 0 || idx >= layerTabs.size()) return null;
        return layerTabs.get(idx);
    }

    public List<LayerTab> getAllTabs() { return layerTabs; }

    public void refreshTabHeaders() {
        tabComponents.forEach(LayerTabComponent::refresh);
    }

    // =================== Status ===================

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public void setStatusRight(String text) {
        SwingUtilities.invokeLater(() -> statusRight.setText(text));
    }

    public void updateTitle(Project project) {
        String name = project.getMetadata() != null ? project.getMetadata().getName() : "New Project";
        String dirty = project.isDirty() ? " *" : "";
        SwingUtilities.invokeLater(() -> setTitle("PCB Laser Studio  –  " + name + dirty));
    }

    // =================== Callbacks ===================

    public void setOnTabChanged(Consumer<Integer> cb)     { this.onTabChanged = cb; }
    public void setOnCloseRequested(Consumer<Void> cb)    { this.onCloseRequested = cb; }

    // =================== Menu ===================

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(DarkTheme.BG_DARKEST);
        menuBar.setBorderPainted(false);

        // File
        JMenu mFile = darkMenu("File");
        menuNew           = darkItem("New Project",           "Ctrl+N");
        menuOpen          = darkItem("Open Project…",         "Ctrl+O");
        menuSave          = darkItem("Save Project",          "Ctrl+S");
        menuSaveAs        = darkItem("Save Project As…",      "Ctrl+Shift+S");
        menuOpenSvg       = darkItem("Open SVG in Active Tab…","Ctrl+I");
        menuClearSvg      = darkItem("Clear SVG from Active Tab", null);
        menuExit          = darkItem("Exit",                  "Alt+F4");

        mFile.add(menuNew);
        mFile.add(menuOpen);
        mFile.addSeparator();
        mFile.add(menuSave);
        mFile.add(menuSaveAs);
        mFile.addSeparator();
        mFile.add(menuOpenSvg);
        mFile.add(menuClearSvg);
        mFile.addSeparator();
        mFile.add(menuExit);

        // Generate
        JMenu mGen = darkMenu("Generate");
        menuGenActive     = darkItem("Generate Active Layer",  "F5");
        menuGenAll        = darkItem("Generate All Layers",    "Ctrl+F5");
        menuExportGcode   = darkItem("Export GCODE…",          "Ctrl+E");
        menuExportProject = darkItem("Export Project…",        null);

        mGen.add(menuGenActive);
        mGen.add(menuGenAll);
        mGen.addSeparator();
        mGen.add(menuExportGcode);
        mGen.add(menuExportProject);

        // Help
        JMenu mHelp = darkMenu("Help");
        menuAbout = darkItem("About PCB Laser Studio", null);
        mHelp.add(menuAbout);

        menuBar.add(mFile);
        menuBar.add(mGen);
        menuBar.add(mHelp);
        setJMenuBar(menuBar);
    }

    private static JMenu darkMenu(String title) {
        JMenu m = new JMenu(title);
        m.setFont(DarkTheme.FONT_NORMAL);
        m.setForeground(DarkTheme.TEXT_PRIMARY);
        m.setBackground(DarkTheme.BG_DARKEST);
        m.getPopupMenu().setBackground(DarkTheme.BG_MEDIUM);
        m.getPopupMenu().setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHT, 1));
        return m;
    }

    private static JMenuItem darkItem(String text, String accel) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(DarkTheme.FONT_NORMAL);
        item.setForeground(DarkTheme.TEXT_PRIMARY);
        item.setBackground(DarkTheme.BG_MEDIUM);
        if (accel != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(accel.replace("Ctrl", "ctrl")
                    .replace("Alt", "alt").replace("Shift", "shift").replace("+", " ")));
        }
        return item;
    }
}

