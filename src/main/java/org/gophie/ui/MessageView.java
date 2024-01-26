/*
    This file is part of Gophie.

    Gophie is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gophie is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gophie. If not, see <https://www.gnu.org/licenses/>.

*/

package org.gophie.ui;

import org.gophie.config.ConfigFile;
import org.gophie.config.ConfigurationManager;
import org.gophie.ui.event.MessageViewListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MessageView extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;
    private static final String MESSAGEVIEW_BACKGROUND_COLOR = "#fcba03";
    private static final String MESSAGEVIEW_FOREGROUND_COLOR = "#000000";

    /* local components */
    private final JLabel messageIcon;
    private final JLabel messageText;
    private final JPanel buttonPanel;
    private final Font iconFont;

    /**
     * Constructs the message view and does not show it by default as one of the
     * show-methods should be called
     */
    public MessageView() {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        /* get the icon font for this navigation bar */
        iconFont = ConfigurationManager.getIconFont(19f);

        /* set box layout for this message view */
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 16));
        setBackground(Color.decode(configFile.getSetting
                ("MESSAGEVIEW_BACKGROUND_COLOR", "Appearance",
                        MESSAGEVIEW_BACKGROUND_COLOR)));

        /* create the label instance */
        messageIcon = new JLabel();
        messageIcon.setFont(iconFont);
        messageIcon.setBorder(new EmptyBorder(0, 5, 0, 10));
        messageIcon.setForeground(Color.decode(configFile.getSetting
                ("MESSAGEVIEW_FOREGROUND_COLOR", "Appearance",
                        MESSAGEVIEW_FOREGROUND_COLOR)));

        messageText = new JLabel();
        messageText.setFont(ConfigurationManager.getDefaultFont(11f));
        messageText.setForeground(Color.decode(configFile.getSetting
                ("MESSAGEVIEW_FOREGROUND_COLOR", "Appearance",
                        MESSAGEVIEW_FOREGROUND_COLOR)));

        buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(0, 30, 0, 0));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(Color.decode(configFile.getSetting
                ("MESSAGEVIEW_BACKGROUND_COLOR", "Appearance",
                        MESSAGEVIEW_BACKGROUND_COLOR)));

        add(messageIcon, BorderLayout.WEST);
        add(messageText, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);

        /* do not show by default */
        setVisible(false);
    }

    /**
     * Creates a new button for this view
     *
     * @param text The text on the button as string
     * @return The button in the form of a JLabel
     */
    private JLabel createButton(String text) {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String buttonColor = configFile.getSetting("MESSAGEVIEW_FOREGROUND_COLOR",
                "Appearance", MESSAGEVIEW_FOREGROUND_COLOR);

        JLabel customButton = new JLabel("<html><div style=\"border:1px solid " + buttonColor + ";"
                + "padding:2px 6px 2px 6px;border-radius:6px;\">" + text + "</div></html>");
        customButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        customButton.setFont(ConfigurationManager.getDefaultFont(11f));
        customButton.setBorder(new EmptyBorder(0, 5, 0, 5));
        customButton.setForeground(Color.decode(buttonColor));
        return customButton;
    }

    /**
     * Shows an information message
     *
     * @param text Text of the information message as string
     */
    public void showInfo(String text) {
        /* build up icon and text */
        messageIcon.setText("");
        messageText.setText(text);
        buttonPanel.removeAll();

        /* add the handle to close this message */
        JLabel closeButton = createButton("Close");
        closeButton.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                setVisible(false);
            }
        });

        /* create and set to visible */
        buttonPanel.add(closeButton);
        setVisible(true);
    }

    /**
     * Shows a confirm message with options
     *
     * @param text       The text to show in the message view
     * @param optionList A string array with the options
     */
    public void showConfirm(String text, String[] optionList, MessageViewListener eventListener) {
        /* remove all components */
        messageIcon.setText("");
        messageText.setText(text);

        /* build the option buttons */
        buttonPanel.removeAll();
        for (int i = 0; i < optionList.length; i++) {
            JLabel optionButton = createButton(optionList[i]);
            final int optionId = i;
            optionButton.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent evt) {
                    eventListener.optionSelected(optionId);
                }
            });

            buttonPanel.add(optionButton);
        }

        setVisible(true);
    }
}