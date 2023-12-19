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
import org.gophie.config.SystemUtility;
import org.gophie.net.DownloadItem;
import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.net.GopherItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DownloadItemRenderer extends JPanel implements ListCellRenderer<DownloadItem> {
    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel = new JLabel();
    private final JLabel textLabel = new JLabel();

    @Override
    public Component getListCellRendererComponent(JList<? extends DownloadItem> list,
                                                  DownloadItem value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        /* get the config file for the color schemes */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        /* render the cell for this download item */
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(5, 10, 5, 10));

        /* highlight if this element is selected */
        if (isSelected) {
            setOpaque(true);
            setBackground(Color.decode(configFile.getSetting
                    ("DOWNLOAD_SELECTED_COLOR", "Appearance", "#cf9a0c")));
        }

        /* get the gopher item of this download */
        GopherItem item = value.getGopherItem();

        /* show the file name in the title */
        titleLabel.setText(item.getFileName());
        Font titleFont = ConfigurationManager.getConsoleFont(15f);
        titleLabel.setFont(titleFont.deriveFont(titleFont.getStyle() | Font.BOLD));
        titleLabel.setForeground(Color.decode(configFile.getSetting
                ("DOWNLOAD_TITLE_COLOR", "Appearance", "#ffffff")));

        /* create the information text based on the status */
        String statusText = "Download not started";
        String byteLoadedText = SystemUtility.getFileSizeString(value.getByteCountLoaded());

        /* show message for completed downloads */
        if (value.getStatus() == DownloadStatus.COMPLETED) {
            statusText = "Completed (" + byteLoadedText + ")";
        }

        /* show message for active downloads */
        if (value.getStatus() == DownloadStatus.ACTIVE) {
            String transferRate = SystemUtility.getFileSizeString(value.getBytePerSecond());
            statusText = byteLoadedText + " (" + transferRate + "/sec)";
        }

        /* show message for failed downloads */
        if (value.getStatus() == DownloadStatus.FAILED) {
            statusText = "Failed (" + byteLoadedText + ")";
        }

        /* append the host name to the info text */
        statusText += " — " + item.getHostName();

        /* set the text to the status text label */
        textLabel.setText(statusText);
        textLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        textLabel.setForeground(Color.decode(configFile.getSetting
                ("DOWNLOAD_TEXT_COLOR", "Appearance", "#e0e0e0")));
        Font textFont = ConfigurationManager.getConsoleFont(13f);
        textLabel.setFont(textFont);

        add(titleLabel);
        add(textLabel);

        return this;
    }
}