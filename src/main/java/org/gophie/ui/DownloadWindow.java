package org.gophie.ui;

import org.gophie.config.ConfigFile;
import org.gophie.config.ConfigurationManager;
import org.gophie.net.DownloadItem;
import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.net.DownloadList;
import org.gophie.net.event.DownloadListEventListener;
import org.gophie.ui.event.ActionButtonEventListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class DownloadWindow implements ActionButtonEventListener {
    private static final String ACTIONBAR_BACKGROUND = "#248AC2";
    private static final String ACTIONBAR_TEXTCOLOR = "#ffffff";
    private static final String ACTIONBAR_INACTIVE_TEXTCOLOR = "#76bce3";
    private static final String FILELIST_BACKGROUND = "#1b1b1b";

    /* local objects */
    private final DownloadList list;
    private DownloadItem[] data;

    /* local components */
    private final JDialog frame;
    private final JList<DownloadItem> fileListView;
    private final JPanel actionBar = new JPanel();
    private final ActionButton clearButton;
    private final ActionButton actionButton;

    public DownloadWindow(DownloadList downloadList) {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        list = downloadList;
        list.addEventListener(new DownloadListEventListener() {
            @Override
            public void downloadListUpdated() {
                updateList();
            }

            @Override
            public void downloadProgressReported() {
                handleSelectionChange();
                frame.repaint();
            }
        });

        frame = new JDialog();
        frame.setTitle("Downloads");
        frame.setMinimumSize(new Dimension(400, 200));
        frame.setLayout(new BorderLayout());
        frame.setIconImage(ConfigurationManager.getImage("icon.png"));

        fileListView = new JList<DownloadItem>();
        fileListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileListView.setCellRenderer(new DownloadItemRenderer());
        fileListView.setFixedCellWidth(fileListView.getWidth());
        fileListView.setOpaque(true);
        fileListView.setBackground(Color.decode(configFile.getSetting
                ("FILELIST_BACKGROUND", "Appearance", FILELIST_BACKGROUND)));

        JScrollPane listScrollPane = new JScrollPane(fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        frame.add(listScrollPane, BorderLayout.CENTER);

        clearButton = new ActionButton("", "Clear List",
                configFile.getSetting("ACTIONBAR_TEXTCOLOR", "Appearance", ACTIONBAR_TEXTCOLOR),
                configFile.getSetting("ACTIONBAR_INACTIVE_TEXTCOLOR", "Appearance", ACTIONBAR_INACTIVE_TEXTCOLOR)
        );
        clearButton.setButtonEnabled(false);
        clearButton.setButtonId(1);
        clearButton.addEventListener(this);

        actionButton = new ActionButton("", "Abort",
                configFile.getSetting("ACTIONBAR_TEXTCOLOR", "Appearance", ACTIONBAR_TEXTCOLOR),
                configFile.getSetting("ACTIONBAR_INACTIVE_TEXTCOLOR", "Appearance", ACTIONBAR_INACTIVE_TEXTCOLOR)
        );
        actionButton.setButtonId(0);
        actionButton.addEventListener(this);

        actionBar.setLayout(new BorderLayout());
        actionBar.setBorder(new EmptyBorder(8, 16, 10, 16));
        actionBar.setBackground(Color.decode(configFile.getSetting
                ("ACTIONBAR_BACKGROUND", "Appearance", ACTIONBAR_BACKGROUND)));
        actionBar.add(clearButton, BorderLayout.EAST);
        actionBar.add(actionButton, BorderLayout.WEST);
        frame.add(actionBar, BorderLayout.SOUTH);

        /* hide the action button for empty lists */
        actionButton.setVisible(false);

        fileListView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleSelectionChange();
            }
        });

        /* update the list for the first time */
        updateList();
    }

    private void handleSelectionChange() {
        DownloadItem selected = fileListView.getSelectedValue();
        if (selected == null) {
            actionButton.setVisible(false);
        } else {
            if (selected.getStatus() == DownloadStatus.ACTIVE) {
                actionButton.setContent("", "Abort");
            }
            if (selected.getStatus() == DownloadStatus.FAILED) {
                actionButton.setContent("", "Retry");
            }
            if (selected.getStatus() == DownloadStatus.COMPLETED) {
                actionButton.setContent("", "Open");
            }
            if (selected.getStatus() == DownloadStatus.IDLE) {
                actionButton.setContent("", "Start");
            }

            actionButton.setVisible(true);
            actionButton.setButtonEnabled(true);
        }

        /* disable the clear list button for empty lists */
        clearButton.setButtonEnabled(list.hasNonActiveItems());
    }

    public void updateList() {
        data = list.getDownloadItemArray();

        int selectedIndex = fileListView.getSelectedIndex();
        fileListView.setListData(data);

        if (selectedIndex < data.length) {
            fileListView.setSelectedIndex(selectedIndex);
        } else {
            if (data.length > 0) {
                fileListView.setSelectedIndex(data.length - 1);
            }
        }

        handleSelectionChange();
    }

    public boolean isVisible() {
        return frame.isVisible();
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void show(JFrame parent) {
        updateList();
        frame.setLocationRelativeTo(parent);
        frame.setVisible(true);
    }

    @Override
    public void buttonPressed(int buttonId) {
        if (buttonId == 0) {
            /* the action button */
            DownloadItem item = fileListView.getSelectedValue();
            if (item.getStatus() == DownloadStatus.ACTIVE) {
                /* cancel the currently active item */
                item.cancel();

                /* remove the item from the list */
                list.remove(item);

                /* delete the file form disk */
                item.deleteFile();
            }
            if (item.getStatus() == DownloadStatus.FAILED) {
                /* retry failed item */
                item.start();
            }
            if (item.getStatus() == DownloadStatus.COMPLETED) {
                /* open completed item file */
                item.openFileOnDesktop();
            }
            if (item.getStatus() == DownloadStatus.IDLE) {
                /* start item in idle item */
                item.start();
            }
        }
        if (buttonId == 1) {
            /* the clear list button */
            list.clearNonActiveItems();
        }

        /* update our local list */
        updateList();
    }
}