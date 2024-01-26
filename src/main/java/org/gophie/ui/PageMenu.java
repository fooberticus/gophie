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

import lombok.extern.slf4j.Slf4j;
import org.gophie.config.ConfigFile;
import org.gophie.config.ConfigurationManager;
import org.gophie.net.GopherItem;
import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.net.GopherPage;
import org.gophie.net.GopherUrl;
import org.gophie.ui.event.PageMenuEventListener;
import org.gophie.ui.util.ImageTransferable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;

@Slf4j
public class PageMenu extends PopupMenu {
    @Serial
    private static final long serialVersionUID = 1L;

    /* the menu items */
    private final MenuItem saveItem;
    private final MenuItem saveTargetItem;
    private final MenuItem copyTargetUrl;
    private final MenuItem copyTargetText;
    private final MenuItem copyImageUrl;
    private final MenuItem copyImageObject;
    private final MenuItem copySelectedItem;
    private final MenuItem selectAllItem;
    private final MenuItem setHomeGopherItem;
    private final PopupMenu copyMenu;

    /* private variables */
    private String selectedText = "";
    private GopherItem targetLink;
    private GopherPage currentPage;

    /* list with event listeners to report to */
    private final ArrayList<PageMenuEventListener> eventListenerList = new ArrayList<PageMenuEventListener>();

    /**
     * Constructs the page menu
     */
    public PageMenu() {
        super();

        /**
         * Check if gopher item prefixes are enabled for urls
         * and add the prefixes to the url copy methods when enabled
         */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String prefixEnabled = configFile.getSetting("SELECTOR_PREFIX_ENABLED", "Navigation", "yes");

        /* request listeners to save the current page as file */
        saveItem = new MenuItem("Save Page As...");
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    for (PageMenuEventListener listener : eventListenerList) {
                        listener.pageSaveRequested(currentPage);
                    }
                }
            }
        });

        /* request listeners to download the file behind the link */
        saveTargetItem = new MenuItem("Save Link As...");
        saveTargetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (targetLink != null) {
                    for (PageMenuEventListener listener : eventListenerList) {
                        listener.itemDownloadRequested(targetLink);
                    }
                }
            }
        });

        /* copies the url of the link target to the clipboard */
        copyTargetUrl = new MenuItem("Copy Link URL");
        copyTargetUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (targetLink != null) {
                    /* use the plain url without prefix by default */
                    String targetLinkUrl = targetLink.getUrlString();

                    if (prefixEnabled.equals("yes")) {
                        /* create the gopher url object for the address */
                        GopherUrl prefixUrl = new GopherUrl(targetLink.getUrlString());
                        prefixUrl.setTypePrefix(targetLink.getItemTypeCode());

                        /* set the address to the url with the prefix */
                        targetLinkUrl = prefixUrl.getUrlString(true);
                    }

                    /* copy the address to the clipboard */
                    copyToClipboard(targetLinkUrl);
                }
            }
        });

        /* copies the url of an individually displayed image */
        copyImageUrl = new MenuItem("Copy Image URL");
        copyImageUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    /* use the plain url without prefix by default */
                    String currentPageUrl = currentPage.getUrl().getUrlString();

                    if (prefixEnabled.equals("yes")) {
                        /* create the gopher url object for the address */
                        GopherUrl prefixUrl = new GopherUrl(currentPage.getUrl().getUrlString());
                        prefixUrl.setTypePrefix(GopherItem.getTypeCode(currentPage.getContentType()));

                        /* set the address to the url with the prefix */
                        currentPageUrl = prefixUrl.getUrlString(true);
                    }

                    /* copy the current page url to the clipboard */
                    copyToClipboard(currentPageUrl);
                }
            }
        });

        /* copies the object of the image to clipboard */
        copyImageObject = new MenuItem("Copy Image");
        copyImageObject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    copyImageToClipboard();
                }
            }
        });

        /* copies the text of the active link to the clipboard */
        copyTargetText = new MenuItem("Copy Link Text");
        copyTargetText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (targetLink != null) {
                    copyToClipboard(targetLink.getUserDisplayString());
                }
            }
        });

        /* copies the currently selected text to the clipboard */
        copySelectedItem = new MenuItem("Copy Selection");
        copySelectedItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedText.length() > 0) {
                    copyToClipboard(selectedText);
                }
            }
        });

        /* requests event listeners to select all text */
        selectAllItem = new MenuItem("Select All");
        selectAllItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (PageMenuEventListener listener : eventListenerList) {
                    listener.selectAllTextRequested();
                }
            }
        });

        /* requests listeners to set current page as home page */
        setHomeGopherItem = new MenuItem("Set As Home Gopher");
        setHomeGopherItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    for (PageMenuEventListener listener : eventListenerList) {
                        listener.setHomeGopherRequested(currentPage.getUrl().getUrlString());
                    }
                }
            }
        });

        /* create the copy menu with its sub-items */
        copyMenu = new PopupMenu("Copy");

        /* copies the url of the current page to the clipboard */
        MenuItem copyUrlItem = new MenuItem("URL");
        copyUrlItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    copyToClipboard(currentPage.getUrl().getUrlString());
                }
            }
        });

        /* copies the text of the current page to the clipboard */
        MenuItem copyTextItem = new MenuItem("Page Text");
        copyTextItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    copyToClipboard(currentPage.getTextContent());
                }
            }
        });

        /* copies the source code of the page to the clipboard */
        MenuItem copySourceItem = new MenuItem("Source Code");
        copySourceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage != null) {
                    copyToClipboard(currentPage.getSourceCode());
                }
            }
        });

        /* add the items to the copy menu */
        copyMenu.add(copyUrlItem);
        copyMenu.add(copyTextItem);
        copyMenu.add(copySourceItem);
    }

    /**
     * Adds and event listener for page menu events
     *
     * @param listener the listener to add to the list
     */
    public void addPageMenuEventListener(PageMenuEventListener listener) {
        eventListenerList.add(listener);
    }

    @Override
    public void show(Component origin, int x, int y) {
        /* remove all items */
        removeAll();

        /* show menu item based on context */
        if (targetLink == null) {
            /* we do not have a link target */
            Boolean isImage = false;

            /* determine the text for the save item */
            if (currentPage != null) {
                switch (currentPage.getContentType()) {
                    case GOPHERMENU:
                        /* save gopher menu as */
                        saveItem.setLabel("Save Page As ...");
                        break;
                    case IMAGE_FILE:
                        /* save image file as */
                        isImage = true;
                        break;
                    case GIF_FILE:
                        /* save image file as */
                        isImage = true;
                        break;
                    default:
                        /* save file as is the generic label */
                        saveItem.setLabel("Save File As ...");
                        break;
                }
            }

            /* set the proper label for image files */
            if (isImage) {
                saveItem.setLabel("Save Image As ...");
            }

            add(saveItem);
            addSeparator();

            /* only show selection copy, when selection exists */
            if (!selectedText.isEmpty() && !isImage) {
                add(copySelectedItem);
            }

            if (!isImage) {
                add(selectAllItem);
                add(copyMenu);
                addSeparator();
                
                /* only allow setting as home gopher when
                    this page is a gopher menu page */
                if (currentPage.getContentType() == GopherItemType.GOPHERMENU) {
                    addSeparator();
                    add(setHomeGopherItem);
                }
            } else {
                add(copyImageObject);
                add(copyImageUrl);
            }
        } else {
            /* we do have a link target */
            add(saveTargetItem);
            addSeparator();
            add(copyTargetUrl);

            /* only show selection copy, when selection exists */
            if (!selectedText.isEmpty()) {
                add(copySelectedItem);
            }

            add(copyTargetText);
        }

        /* call the base method */
        super.show(origin, x, y);
    }

    /**
     * Copies the current image in the page to the clipboard
     */
    private void copyImageToClipboard() {
        if (currentPage != null) {
            if (currentPage.getContentType() == GopherItemType.IMAGE_FILE
                    || currentPage.getContentType() == GopherItemType.GIF_FILE) {
                /* seems to be a valid image file, copy it to clipboard */
                try {
                    InputStream imageInputStream = new ByteArrayInputStream(currentPage.getByteArray());
                    BufferedImage bufferedImage = ImageIO.read(imageInputStream);
                    ImageTransferable transferImage = new ImageTransferable(bufferedImage);
                    Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipBoard.setContents(transferImage, null);
                } catch (Exception ex) {
                    /* output information about the clippy failure */
                    log.error("Unable to copy image to clipboard: {}", ex.getMessage());
                }
            }
        }
    }

    /**
     * Copies the text provided to the clipboard
     *
     * @param text text to copy to the clipboard
     */
    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents((new StringSelection(text)), null);
    }

    public void setCurrentPage(GopherPage value) {
        /* reset the link target when a new page was loaded */
        targetLink = null;

        /* set the current page locally */
        currentPage = value;
    }

    public void setLinkTarget(GopherItem value) {
        targetLink = value;
    }

    public void setSelectedText(String value) {
        if (value == null) {
            selectedText = "";
        } else {
            selectedText = value;
        }
    }
}