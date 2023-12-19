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

package org.gophie.net;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.net.event.DownloadItemEventListener;
import org.gophie.net.event.GopherClientEventListener;
import org.gophie.net.event.GopherError;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

@Slf4j
public class DownloadItem implements GopherClientEventListener {
    /* local objects and variables */
    private GopherItem item;
    private final GopherClient client;
    private String fileName;
    private Boolean openFile = false;
    /**
     * -- GETTER --
     *  Returns the number of bytes loaded
     *
     * @return The number of bytes loaded as long
     */
    @Getter
    private long byteCountLoaded = 0;
    /**
     * -- GETTER --
     *  Returns the status of this download
     *
     * @return The status as DownloadStatus enum
     */
    @Getter
    private DownloadStatus status = DownloadStatus.IDLE;
    private final ArrayList<DownloadItemEventListener> eventListenerList = new ArrayList<DownloadItemEventListener>();
    /*
     * local variables for calculating the bit rate at which the download currently
     * operates
     */
    private long startTimeMillis = 0;
    @Getter
    private long bytePerSecond = 0;
    /**
     * Constructor creates the download and starts it immediately
     *
     * @param gopherItem       The gopher item to download
     * @param targetFile       The file to write the contents to
     * @param openWhenFinished If true, opens the file when finished
     */
    public DownloadItem(GopherItem gopherItem, String targetFile, Boolean openWhenFinished) {
        client = new GopherClient();
        item = gopherItem;
        fileName = targetFile;
        openFile = openWhenFinished;
        start();
    }

    /**
     * Constructor creates the item with default values and does not do anything
     */
    public DownloadItem() {
        client = new GopherClient();
    }

    public void addEventListener(DownloadItemEventListener listener) {
        eventListenerList.add(listener);
    }

    private void notifyProgress() {
        for (DownloadItemEventListener listener : eventListenerList) {
            listener.downloadProgressReported();
        }
    }

    /**
     * Starts the download of the file
     */
    public void start() {
        /* start the download process */
        String url = new GopherUrl(item.getUrlString()).getUrlString();
        client.downloadAsync(url, fileName, this);
        status = DownloadStatus.ACTIVE;
    }

    /**
     * Tries to delete the file
     */
    public void deleteFile() {
        try {
            File file = new File(fileName);
            file.delete();
        } catch (Exception ex) {
            /* just log the error and keep the crap, what else to do? */
            log.error("Failed to delete downloaded file ({}): {}", fileName, ex.getMessage());
        }
    }

    /**
     * Cancels this download
     */
    public void cancel() {
        client.cancelFetch();
    }

    /**
     * Sets the target file to download to
     *
     * @param targetFile Path of the file to store data in
     */
    public void setTargetFile(String targetFile) {
        fileName = targetFile;
    }

    /**
     * Returns the gopher item
     *
     * @return Returns the gopher item to download
     */
    public GopherItem getGopherItem() {
        return item;
    }

    /**
     * Sets the gopher item
     *
     * @param gopherItem The gopher item to download
     */
    public void setGopherItem(GopherItem gopherItem) {
        item = gopherItem;
    }

    /**
     * Opens the file on the users desktop
     */
    public void openFileOnDesktop() {
        try {
            /* use the desktop to open the file */
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(fileName));
            } else {
                /* no desktop support here, report one level up */
                throw new Exception("Desktop not supported");
            }
        } catch (Exception ex) {
            /* output the exception that the file could not be opened */
            log.error("Unable to open file after download ({}): {}", fileName, ex.getMessage());
        }
    }

    @Override
    public void progress(GopherUrl url, long byteCount) {
        /* set start time to calculate total duration */
        if (startTimeMillis == 0) {
            /* use time in milliseconds */
            startTimeMillis = System.currentTimeMillis();
        }

        /* calculate the bitrate of the download */
        long timeNow = System.currentTimeMillis();
        long duration = ((timeNow - startTimeMillis) / 1000);
        if (duration > 0 && byteCount > 0) {
            bytePerSecond = (byteCount / duration);
        }

        /* update the local byte counter */
        byteCountLoaded = byteCount;

        notifyProgress();
    }

    @Override
    public void pageLoaded(GopherPage result) {
        /* set the status to complete */
        status = DownloadStatus.COMPLETED;

        /* check if file open was requested */
        if (openFile) {
            openFileOnDesktop();
        }

        notifyProgress();
    }

    @Override
    public void pageLoadFailed(GopherError error, GopherUrl url) {
        status = DownloadStatus.FAILED;
        notifyProgress();
    }

    @Override
    public void pageLoadItemMismatch(GopherItemType requested, GopherItemType detected, GopherUrl url) {
        /* this should not be the case for the download manager
            as downloads would not raise item mismatch events */
    }

    public enum DownloadStatus {
        IDLE, FAILED, ACTIVE, COMPLETED
    }
}