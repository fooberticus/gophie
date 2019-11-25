package org.gophie.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gophie.net.*;
import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.net.event.*;

public class DownloadWindow {
    private static final String ACTIONBAR_BACKGROUND = "#248AC2";
    private static final String ACTIONBAR_TEXTCOLOR = "#ffffff";
    private static final String ACTIONBAR_INACTIVE_TEXTCOLOR = "#76bce3";
    private static final String FILELIST_BACKGROUND = "#1b1b1b";

    /* local objects */
    private DownloadList list;
    private DownloadItem[] data;

    /* local components */
    private JDialog frame;
    private JList<DownloadItem> fileListView;
    private JPanel actionBar = new JPanel();
    private ActionButton clearButton;
    private ActionButton actionButton;

    public DownloadWindow(DownloadList downloadList) {
        this.list = downloadList;
        this.list.addEventListener(new DownloadListEventListener() {
            @Override
            public void downloadListUpdated() {
                updateList();
            }

            @Override
            public void downloadProgressReported() {
                frame.repaint();
            }
        });

        this.frame = new JDialog();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400, 200));
        this.frame.setLayout(new BorderLayout());

        this.fileListView = new JList<DownloadItem>();
        this.fileListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.fileListView.setCellRenderer(new DownloadItemRenderer());
        this.fileListView.setFixedCellWidth(this.fileListView.getWidth());
        this.fileListView.setOpaque(true);
        this.fileListView.setBackground(Color.decode(FILELIST_BACKGROUND));

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);

        this.clearButton = new ActionButton("", "Clear List",
            ACTIONBAR_TEXTCOLOR,ACTIONBAR_INACTIVE_TEXTCOLOR);
        this.clearButton.setEnabled(false);

        this.actionButton = new ActionButton("", "Abort",
            ACTIONBAR_TEXTCOLOR,ACTIONBAR_INACTIVE_TEXTCOLOR);

        this.actionBar.setLayout(new BorderLayout());
        this.actionBar.setBorder(new EmptyBorder(8, 16, 10, 16));
        this.actionBar.setBackground(Color.decode(ACTIONBAR_BACKGROUND));
        this.actionBar.add(this.clearButton, BorderLayout.EAST);
        this.actionBar.add(this.actionButton, BorderLayout.WEST);
        this.frame.add(this.actionBar, BorderLayout.SOUTH);

        /* hide the action button for empty lists */
        this.actionButton.setVisible(false);

        this.fileListView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                DownloadItem selected = fileListView.getSelectedValue();
                if(selected == null){
                    actionButton.setVisible(false);
                }else{
                    if(selected.getStatus() == DownloadStatus.ACTIVE){
                        actionButton.setContent("","Abort");
                    }if(selected.getStatus() == DownloadStatus.FAILED){
                        actionButton.setContent("","Retry");
                    }if(selected.getStatus() == DownloadStatus.COMPLETED){
                        actionButton.setContent("","Open");
                    }if(selected.getStatus() == DownloadStatus.IDLE){
                        actionButton.setContent("","Start");
                    }

                    actionButton.setVisible(true);
                }
            }        
        });

        /* update the list for the first time */
        this.updateList();
    }

    public void updateList(){
        this.data = this.list.getDownloadItemArray();

        /* disable the clear list button for empty lists */
        if(this.list.hasNonActiveItems()){
            this.clearButton.setEnabled(false);
        }

        int selectedIndex = this.fileListView.getSelectedIndex();
        this.fileListView.setListData(this.data);

        if(selectedIndex < this.data.length){
            this.fileListView.setSelectedIndex(selectedIndex);
        }else{
            if(this.data.length > 0){
                this.fileListView.setSelectedIndex(this.data.length-1);
            }
        }
    }

    public boolean isVisible(){
        return this.frame.isVisible();
    }

    public void hide(){
        this.frame.setVisible(false);
    }

    public void show(JFrame parent){
        this.updateList();
        this.frame.setLocationRelativeTo(parent);
        this.frame.setVisible(true);
    }
}