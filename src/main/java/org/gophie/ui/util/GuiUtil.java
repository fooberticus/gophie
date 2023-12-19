package org.gophie.ui.util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.IntelliJTheme;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatNightOwlContrastIJTheme;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

@Slf4j
public final class GuiUtil {

    public static final String APPLICATION_NAME = "Gophie";
    public static final String DEFAULT_THEME = "Material Darker Contrast";
    private static final String NAMESPACE = "org.gophie.Gophie";
    private static final int DEFAULT_FONT_SIZE = 18;

    private static final Preferences prefs;
    private static final Map<String, Class<? extends IntelliJTheme.ThemeLaf>> themeMap;
    private static final String[] themeNames;
    private static final Image trayIconImage;

    private static final String PROPERTY_FONT_SIZE = "globalFontSize";
    private static final String PROPERTY_THEME_NAME = "themeName";
    private static final String PROPERTY_WINDOW_WIDTH = "windowWidth";
    private static final String PROPERTY_WINDOW_HEIGHT = "windowHeight";
    private static final String PROPERTY_WINDOW_X = "windowX";
    private static final String PROPERTY_WINDOW_Y = "windowY";

    static {
        prefs = Preferences.userRoot().node(NAMESPACE);

        themeMap = new HashMap<>();
        themeMap.put("Nord", FlatNordIJTheme.class);
        themeMap.put("Material Dark", FlatMaterialDesignDarkIJTheme.class);
        themeMap.put("Arc Dark Orange", FlatArcDarkOrangeIJTheme.class);
        themeMap.put("Spacegray", FlatSpacegrayIJTheme.class);
        themeMap.put("Night Owl", FlatNightOwlContrastIJTheme.class);
        themeMap.put("Material Oceanic", FlatMaterialOceanicIJTheme.class);
        themeMap.put("XCode Dark", FlatXcodeDarkIJTheme.class);
        themeMap.put("Monokai Pro", FlatMonokaiProIJTheme.class);
        themeMap.put("High Contrast", FlatHighContrastIJTheme.class);
        themeMap.put("Material Darker Contrast", FlatMaterialDarkerContrastIJTheme.class);

        themeNames = themeMap.keySet().toArray(new String[0]);
        Arrays.sort(themeNames);

        trayIconImage = Toolkit.getDefaultToolkit()
                .createImage( GuiUtil.class.getResource("/svg/app_icon.svg") );
    }

    private GuiUtil() {}

    /** Call this when the app first starts to initialize the GUI with saved preferences. */
    public static void initGui(JFrame frame) {
        frame.setIconImages(FlatSVGUtils.createWindowIconImages( "/svg/app_icon.svg" ));

        if (getSavedWindowWidth() > 0) {
            frame.setPreferredSize(new Dimension(getSavedWindowWidth(), getSavedWindowHeight()));
        }

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Component c = (Component) e.getSource();
                saveWindowWidth(c.getWidth());
                saveWindowHeight(c.getHeight());
            }

            public void componentMoved(ComponentEvent e) {
                Component c = (Component) e.getSource();
                saveWindowX(c.getX());
                saveWindowY(c.getY());
            }
        });

        if (getSavedWindowX() < 0) {
            frame.setLocationRelativeTo(null);
        } else {
            frame.setLocation(getSavedWindowX(), getSavedWindowY());
        }

        FlatAnimatedLafChange.duration = 300;
        swapTheme(getCurrentTheme());
        UIManager.put("defaultFont", getSavedFont());
        FlatLaf.updateUI();
    }

    /** Display a notification using the native System Tray. */
    public static void showSystemTrayNotification(String msg) {
        showSystemTrayNotification(msg, TrayIcon.MessageType.INFO);
    }

    /** Display an error notification using the native System Tray. */
    public static void showSystemTrayError(String msg) {
        showSystemTrayNotification(msg, TrayIcon.MessageType.ERROR);
    }

    private static void showSystemTrayNotification(String msg, TrayIcon.MessageType type) {
        SystemTray tray = SystemTray.getSystemTray();

        TrayIcon trayIcon = new TrayIcon(trayIconImage, APPLICATION_NAME);
        trayIcon.setImageAutoSize(true);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            JOptionPane
                    .showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        trayIcon.displayMessage(APPLICATION_NAME, msg, type);
    }

    /* FONT UTILITIES */

    public static void increaseGlobalFontSize() {
        setGlobalFontSizeOffset(1);
    }

    public static void decreaseGlobalFontSize() {
        setGlobalFontSizeOffset(-1);
    }

    private static Font getSavedFont() {
        Font font = UIManager.getFont("defaultFont");
        return font.deriveFont((float) prefs.getInt(PROPERTY_FONT_SIZE, DEFAULT_FONT_SIZE));
    }


    private static void setGlobalFontSizeOffset(int offSet) {
        Font newFont = getDefaultFontWithSizeOffset(offSet);
        UIManager.put("defaultFont", newFont);
        FlatLaf.updateUI();
        prefs.putInt(PROPERTY_FONT_SIZE, newFont.getSize());
    }

    private static Font getDefaultFontWithSizeOffset(int offSet) {
        Font font = UIManager.getFont("defaultFont");
        return font.deriveFont((float) (font.getSize() + offSet));
    }

    /* THEME UTILITIES */

    public static String[] getThemeNames() {
        return themeNames;
    }

    public static String getCurrentTheme() {
        return prefs.get(PROPERTY_THEME_NAME, DEFAULT_THEME);
    }

    public static void updateTheme(String themeName) {
        if (!themeMap.containsKey(themeName)) {
            log.error("invalid theme name passed to GuiUtil::updateTheme - {}", themeName);
            return;
        }
        FlatAnimatedLafChange.showSnapshot();
        swapTheme(themeName);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        prefs.put(PROPERTY_THEME_NAME, themeName);
    }

    private static void swapTheme(String themeName) {
        try {
            themeMap.get(themeName).getDeclaredMethod("setup").invoke(null);
        } catch (Exception e) {
            log.error("well that didn't work: {}", e.getMessage());
        }
    }

    /* MAIN WINDOW SIZE UTILITIES */

    public static float getGlobalFontSize() {
        return prefs.getFloat(PROPERTY_FONT_SIZE, 18f);
    }

    public static int getSavedWindowWidth() {
        return prefs.getInt(PROPERTY_WINDOW_WIDTH, 800);
    }

    public static void saveWindowWidth(int width) {
        prefs.putInt(PROPERTY_WINDOW_WIDTH, width);
    }

    public static int getSavedWindowHeight() {
        return prefs.getInt(PROPERTY_WINDOW_HEIGHT, 600);
    }

    public static void saveWindowHeight(int height) {
        prefs.putInt(PROPERTY_WINDOW_HEIGHT, height);
    }

    public static int getSavedWindowX() {
        return prefs.getInt(PROPERTY_WINDOW_X, -1);
    }

    public static void saveWindowX(int x) {
        prefs.putInt(PROPERTY_WINDOW_X, x);
    }

    public static int getSavedWindowY() {
        return prefs.getInt(PROPERTY_WINDOW_Y, -1);
    }

    public static void saveWindowY(int y) {
        prefs.putInt(PROPERTY_WINDOW_Y, y);
    }

}
