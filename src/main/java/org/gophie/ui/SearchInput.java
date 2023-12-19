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
import org.gophie.ui.event.SearchInputListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SearchInput extends JPanel {
    public static final String SEARCH_BACKGROUND = "#248AC2";
    public static final String SEARCH_TITLECOLOR = "#76bce3";
    public static final String SEARCH_TEXTCOLOR = "#e8e8e8";
    /* constants */
    private static final long serialVersionUID = 1L;
    JLabel searchIcon;
    JLabel searchTitle;
    JTextField searchText;

    public SearchInput() {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(new EmptyBorder(6, 12, 12, 14));
        setBackground(Color.decode(configFile.getSetting
                ("SEARCH_BACKGROUND", "Appearance", SEARCH_BACKGROUND)));

        searchIcon = new JLabel("");
        searchIcon.setFont(ConfigurationManager.getIconFont(16f));
        searchIcon.setBorder(new EmptyBorder(0, 0, 0, 8));
        searchIcon.setForeground(Color.decode(configFile.getSetting
                ("SEARCH_TITLECOLOR", "Appearance", SEARCH_TITLECOLOR)));

        add(searchIcon);

        searchTitle = new JLabel("Search");
        searchTitle.setForeground(Color.decode(configFile.getSetting
                ("SEARCH_TITLECOLOR", "Appearance", SEARCH_TITLECOLOR)));

        searchTitle.setBorder(new EmptyBorder(2, 0, 0, 12));
        add(searchTitle);

        searchText = new JTextField();
        searchText.setBorder(new EmptyBorder(2, 0, 0, 0));
        searchText.setBackground(Color.decode(configFile.getSetting
                ("SEARCH_BACKGROUND", "Appearance", SEARCH_BACKGROUND)));

        searchText.setForeground(Color.decode(configFile.getSetting
                ("SEARCH_TEXTCOLOR", "Appearance", SEARCH_TEXTCOLOR)));

        searchText.setCaretColor(Color.decode(configFile.getSetting
                ("SEARCH_TEXTCOLOR", "Appearance", SEARCH_TEXTCOLOR)));

        searchText.setFont(ConfigurationManager.getDefaultFont(14f));
        add(searchText);

        setVisible(false);
    }

    public void performSearch(String title, SearchInputListener listener) {
        searchTitle.setText(title);
        searchTitle.setFont(ConfigurationManager.getDefaultFont(14f));
        searchText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                /* execute search when the ENTER key is pressed */
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    JTextField textField = (JTextField) e.getSource();

                    /* only execute search when text is not empty */
                    if (textField.getText().length() > 0) {
                        listener.searchRequested(textField.getText());
                    }

                    setVisible(false);
                }

                /* just cancel when the user hit ESC */
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                }
            }
        });
        setVisible(true);
        searchText.grabFocus();
    }
}