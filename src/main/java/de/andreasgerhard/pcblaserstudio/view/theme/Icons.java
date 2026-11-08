package de.andreasgerhard.pcblaserstudio.view.theme;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * Small registry of vendored Lucide icons (ISC license, see
 * {@code resources/icons/NOTICE.md}), rendered via FlatLaf's SVG icon support
 * and tinted at runtime to match the current theme.
 */
public final class Icons {

    private Icons() {}

    public static final String ZOOM_IN    = "zoom-in";
    public static final String ZOOM_OUT   = "zoom-out";
    public static final String FIT        = "maximize";
    public static final String EYE        = "eye";
    public static final String EYE_OFF    = "eye-off";
    public static final String PLAY       = "play";
    public static final String PAUSE      = "pause";
    public static final String STOP       = "square";
    public static final String FOLDER_OPEN= "folder-open";
    public static final String SAVE       = "save";
    public static final String DOWNLOAD   = "download";
    public static final String SLIDERS    = "sliders-horizontal";
    public static final String CHEVRON_DOWN  = "chevron-down";
    public static final String CHEVRON_RIGHT = "chevron-right";
    public static final String INFO       = "info";
    public static final String CHECK      = "circle-check";
    public static final String LAYERS     = "layers";
    public static final String PLUG       = "plug-zap";
    public static final String SEND       = "send";
    /** Vertical-axis flip (left/right mirror). */
    public static final String FLIP_X     = "arrow-left-right";
    /** Horizontal-axis flip (top/bottom mirror). */
    public static final String FLIP_Y     = "arrow-up-down";
    public static final String REFRESH    = "refresh-cw";
    public static final String CLOSE      = "x";

    /** Default icon size for compact toolbar/button glyphs. */
    public static final int DEFAULT_SIZE = 14;

    /** Icon tinted with the default dim text color, at {@link #DEFAULT_SIZE}. */
    public static Icon of(String name) {
        return of(name, DEFAULT_SIZE, DarkTheme.TEXT_DIM);
    }

    /** Icon tinted with the default dim text color, at a custom size. */
    public static Icon of(String name, int size) {
        return of(name, size, DarkTheme.TEXT_DIM);
    }

    /** Icon tinted with a specific color, at a custom size. */
    public static Icon of(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }
}
