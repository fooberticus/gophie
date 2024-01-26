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

import lombok.Setter;
import org.gophie.config.ConfigurationManager;
import org.gophie.ui.event.ActionButtonEventListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class ActionButton extends JPanel {
    private static final long serialVersionUID = 1L;

    /* private variables */
    @Setter
    private int buttonId = 0;
    /**
     * -- SETTER --
     *  Sets the color for inactive/disabled button
     *
     * @param colorHex The color in hex format
     */
    @Setter
    private String inactiveTextColor;
    @Setter
    private String textColor;
    private Boolean isEnabledButton = false;
    private final ArrayList<ActionButtonEventListener> eventListenerList = new ArrayList<>();

    /* private components */
    private final JLabel iconLabel;
    private final JLabel textLabel;

    public ActionButton(String iconText, String text, String textColorHex, String inactiveTextColorHex) {
        /* construct the base */
        super();

        /* set text colors locally */
        textColor = textColorHex;
        inactiveTextColor = inactiveTextColorHex;

        /* configure the layout for this button */
        setLayout(new BorderLayout());
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);

        /* icon for the button using the icon font */
        iconLabel = new JLabel(iconText);
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 6));
        iconLabel.setOpaque(false);
        iconLabel.setFont(ConfigurationManager.getIconFont(14f));
        iconLabel.setForeground(Color.decode(inactiveTextColorHex));
        add(iconLabel, BorderLayout.WEST);

        /* text for the button using the default text font */
        textLabel = new JLabel(text);
        textLabel.setOpaque(false);
        textLabel.setFont(ConfigurationManager.getDefaultFont(12f));
        textLabel.setForeground(Color.decode(inactiveTextColorHex));
        add(textLabel, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            /* notify the listeners of the button pressed event */
            public void mouseReleased(MouseEvent evt) {
                /* will be handled by another handler */
                for (ActionButtonEventListener listener : eventListenerList) {
                    listener.buttonPressed(buttonId);
                }
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                /* only show hover effect when button is enabled */
                if (isButtonEnabled()) {
                    iconLabel.setForeground(Color.decode(textColor));
                    textLabel.setForeground(Color.decode(textColor));
                }
            }

            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                iconLabel.setForeground(Color.decode(inactiveTextColor));
                textLabel.setForeground(Color.decode(inactiveTextColor));
            }
        });
    }

    /**
     * Adds a new event listener to this button
     *
     * @param listener The event listener to add to this button
     */
    public void addEventListener(ActionButtonEventListener listener) {
        eventListenerList.add(listener);
    }

    public void setContent(String iconText, String text) {
        iconLabel.setText(iconText);
        textLabel.setText(text);
    }

    /**
     * Enables or disables the button
     *
     * @param value true means enabled, false is otherwise
     */
    public void setButtonEnabled(Boolean value) {
        isEnabledButton = value;

        if (value) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public Boolean isButtonEnabled() {
        return isEnabledButton;
    }
}