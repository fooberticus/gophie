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
import org.gophie.ui.event.NavigationInputListener;
import org.gophie.ui.util.GuiUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * The PageView component renders GopherPage objects
 */
@Slf4j
public class PageView extends JScrollPane {
    /* constants */
    private static final long serialVersionUID = 1L;

    /* local variables and objects */
    private final PageMenu pageMenu;
    private final JEditorPane viewPane;
    private final JEditorPane headerPane;
    private final HTMLEditorKit editorKit;
    private StyleSheet styleSheet;
    private Font textFont;
    private String viewTextColor = "#ffffff";
    private final String selectionColor = "#cf9a0c";

    /* the config file with all settings */
    private final ConfigFile configFile;

    /* listeners for local events */
    private final ArrayList<NavigationInputListener> inputListenerList;

    /* current page displayed */
    private GopherPage currentPage = null;

    /**
     * Constructs the PageView component object
     *
     * @param textColor       The color of the text to display
     * @param backgroundColor The background color of the viewer
     */
    public PageView(MainWindow parent, String textColor, String backgroundColor) {
        /* get the config file to fetch the settings */
        configFile = ConfigurationManager.getConfigFile();

        /* instanciate input listener list */
        inputListenerList = new ArrayList<>();

        /* create the editor kit instance */
        editorKit = new HTMLEditorKit();

        /* create the editor pane */
        viewPane = new JEditorPane() {
            private static final long serialVersionUID = 1L;

            /**
             * Override the scroll tracks so that the content
             * will adjust to the window size horizontally
             */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };

        viewPane.setEditable(false);
        //viewPane.setBackground(Color.decode(backgroundColor));
        //viewPane.setForeground(Color.decode(textColor));
        viewPane.setBorder(new EmptyBorder(10, 4, 8, 16));
        viewPane.setEditorKit(editorKit);
        viewPane.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        //viewPane.setSelectionColor(Color.decode(configFile.getSetting("PAGE_SELECTION_COLOR", "Appearance", selectionColor)));

        viewPane.setDragEnabled(false);
        getViewport().add(viewPane);

        /* adjust the scrollbars */
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        /* create the header pane with line numbers and icons */
        headerPane = new JEditorPane();
        headerPane.setEditable(false);
        //headerPane.setBackground(Color.decode(backgroundColor));
        //headerPane.setForeground(Color.decode(textColor));
        headerPane.setBorder(new EmptyBorder(10, 12, 8, 2));
        headerPane.setEditorKit(editorKit);
        headerPane.setDragEnabled(false);
        setRowHeaderView(headerPane);

        /* configure the style of the header and the view */
        configureStyle();

        /* create the page menu and attach the popup trigger */
        pageMenu = new PageMenu();
        pageMenu.addPageMenuEventListener(parent);
        viewPane.add(pageMenu);
        viewPane.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                switch (evt.getButton()) {
                    case 3: // right mouse context menu
                        pageMenu.setSelectedText(viewPane.getSelectedText());
                        pageMenu.show(viewPane, (int) evt.getPoint().getX(), (int) evt.getPoint().getY());
                        break;
                    case 4: // "back" button on 5+ button mouse
                        inputListenerList.forEach(NavigationInputListener::backwardRequested);
                        break;
                    case 5: // "forward" button on 5+ button mouse
                        inputListenerList.forEach(NavigationInputListener::forwardRequested);
                        break;
                    default:
                        break;
                }
            }
        });

        /* report any links hits as address request to the listeners */
        viewPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                /* get the url of that link */
                String urlValue = e.getDescription();

                /* determine the content type of the link target */
                GopherItem itemObject = null;
                if (currentPage != null) {
                    /* determine the content type of the gopher item
                        by the definition of it in the gopher menu */
                    for (GopherItem contentItem : currentPage.getItemList()) {
                        if (contentItem.getUrlString().equals(urlValue)) {
                            itemObject = contentItem;
                        }
                    }
                }

                /* pass the active link item to the popup menu */
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    pageMenu.setLinkTarget(itemObject);
                }

                /* reset the link target for the popup menu */
                if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    pageMenu.setLinkTarget(null);
                }

                /* handle link activation (aka left-click) */
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    /* execute the handler */
                    for (NavigationInputListener inputListener : inputListenerList) {
                        inputListener.addressRequested(urlValue, itemObject);
                    }
                }
            }
        });

        /* try to open the font for icon display */
        textFont = ConfigurationManager
                .getConsoleFont(ConfigurationManager.getConsoleFontSize(GuiUtil.getGlobalFontSize()));

        /* apply the font settings to the view pane */
        viewPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        viewPane.setFont(textFont);

        /* apply the font settings to the header pane */
        headerPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        headerPane.setFont(textFont);
        headerPane.setHighlighter(null);
    }

    /**
     * Adds a new navigation listener for any navigation events
     *
     * @param listener The listener that responds to navigation events
     */
    public void addListener(NavigationInputListener listener) {
        inputListenerList.add(listener);
    }

    /**
     * Instead of displaying a gopher page, it displays content
     * as plain text in the view page section of this component
     *
     * @param content GopherPage with respective content
     */
    public void showGopherContent(GopherPage content) {
        /* reset the header to just show nothing */
        headerPane.setText("");

        /* set current page to the page menu */
        pageMenu.setCurrentPage(content);

        /* check the type of content supplied */
        if (content.getContentType() == GopherItemType.IMAGE_FILE
                || content.getContentType() == GopherItemType.GIF_FILE) {
            /* try to display as an image */
            try {
                /* try to identify the file extension */
                String imageFileExt = ".jpg";
                if (content.getContentType() == GopherItemType.GIF_FILE) {
                    imageFileExt = ".gif";
                }

                /* try to determine the filetype from the url */
                String imageUrl = content.getUrl().getUrlString();
                if (imageUrl.substring(imageUrl.length() - 4).equals(".")) {
                    imageFileExt = imageUrl.substring(imageUrl.length() - 3);
                }
                if (imageUrl.substring(imageUrl.length() - 5).equals(".")) {
                    imageFileExt = imageUrl.substring(imageUrl.length() - 4);
                }

                /* write the image content to file */
                File tempImageFile = File.createTempFile("gopherimagefile", imageFileExt);
                FileOutputStream outputStream = new FileOutputStream(tempImageFile);
                outputStream.write(content.getByteArray());
                outputStream.close();

                /* determine image size and rescale */
                String imageHtmlCode = "<img src=\"" + tempImageFile.toURI()
                        .toURL().toExternalForm() + "\" />";

                try {
                    BufferedImage bufferedImage = ImageIO.read(tempImageFile);
                    int width = bufferedImage.getWidth();
                    int height = bufferedImage.getHeight();
                    if (width > 800) {
                        double calcHeight = (((double) height) / (((double) width) / 800));
                        imageHtmlCode = "<img src=\"" + tempImageFile.toURI().toURL().toExternalForm() + "\" "
                                + "width=800 height=" + (int) calcHeight + " />";
                    }
                } catch (Exception ex) {
                    /* failed to determine image size */
                    log.error("Failed to determine image size: {}", ex.getMessage());
                }

                /* display content as an image */
                viewPane.setContentType("text/html");
                viewPane.setText(imageHtmlCode);
            } catch (Exception ex) {
                /* display exception cause as text inside the view */
                viewPane.setContentType("text/plain");
                viewPane.setText("Failed to display the image:\n" + ex.getMessage());
            }
        } else {
            /* display content as plain text */
            viewPane.setContentType("text/plain");
            viewPane.setText(content.getSourceCode().replace("\n.\r\n", ""));
        }
    }

    /**
     * Formats the item's title for display in the view
     *
     * @param text the text with unencoded elements
     * @return the text with html-encoded entities
     */
    private String formatItemTitle(String text) {
        return text.replace("&", "&amp;").replace(" ", "&nbsp;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Initialises rendering of a GopherPage on this view
     *
     * @param page The GopherPage to display on this view
     */
    public void showGopherPage(GopherPage page) {
        /* set the current local gopher page */
        currentPage = page;

        /* set current page to the page menu */
        pageMenu.setCurrentPage(page);

        /* create the headers */
        String renderedHeader = "<table cellspacing=\"0\" cellpadding=\"2\">";
        String renderedContent = "<table cellspacing=\"0\" cellpadding=\"2\">";

        int lineNumber = 1;
        for (GopherItem item : page.getItemList()) {
            /* set the content for the row header */
            renderedHeader += "<tr><td class=\"lineNumber\">" + lineNumber + "</td>"
                    + "<td><div class=\"itemIcon\">"
                    + getGopherItemTypeIcon(item.getItemTypeCode())
                    + "</div></td></tr>";

            /* set the content for the text view */
            String itemTitle = formatItemTitle(item.getUserDisplayString());

            if (itemTitle.isEmpty()) {
                itemTitle = "&nbsp;";
            }
            String itemCode = "<span class=\"text\">" + itemTitle + "</span>";

            /* build links for anything other than infromation items */
            if (!item.getItemTypeCode().equals("i")) {
                /* create the link for this item */
                itemCode = "<a href=\"" + item.getUrlString() + "\">" + itemTitle + "</a>";
            }

            /* create the item table row */
            renderedContent += "<tr><td class=\"item\">" + itemCode + "</td></tr>";

            lineNumber++;
        }

        /* set content type and add content to view */
        viewPane.setContentType("text/html");
        viewPane.setText(renderedContent + "</table>");

        /* set content type and add content to header */
        headerPane.setContentType("text/html");
        headerPane.setText(renderedHeader + "</table>");

        /* scroll the view pane to the top */
        viewPane.setCaretPosition(0);
    }

    /**
     * Configures the style of the view
     */
    private void configureStyle() {
        /* get the color schemes from the config file */
        //String linkColor = configFile.getSetting("PAGE_LINK_COLOR", "Appearance", "#22c75c");
        //String lineNumberColor = configFile.getSetting("PAGE_LINENUMBER_COLOR", "Appearance", "#454545");

        /* get the configured icon font size */
        String iconFontSize = ConfigurationManager.getConfigFile().getSetting("PAGE_ICON_FONT_SIZE", "Appearance", "10");

        /* build up the stylesheet for the rendering */
        styleSheet = editorKit.getStyleSheet();
        styleSheet.addRule("body { white-space:nowrap; margin:0; padding:0; vertical-align: top;}");
        styleSheet.addRule(".text { cursor:text; }");
        //styleSheet.addRule(".lineNumber { color: " + lineNumberColor + "; }");
        styleSheet.addRule(".itemIcon { font-family:Feather; font-size:" + iconFontSize + "px; margin-left:5px; }");
        //styleSheet.addRule("a { text-decoration: none; color: " + linkColor + "; }");
    }

    /**
     * Selects all the items in the view
     */
    public void selectAllText() {
        /* just pass it onto the view */
        viewPane.selectAll();
        viewPane.requestFocus();
    }

    /**
     * Returns the header icon for the gopher item type
     *
     * @param code Code for the gopher item type
     * @return String with the icon for the item
     */
    public String getGopherItemTypeIcon(String code) {
        String result = "";

        if (code.equals("0")) {
            result = "";
        }
        if (code.equals("1")) {
            result = "";
        }
        if (code.equals("2")) {
            result = "";
        }
        if (code.equals("3")) {
            result = "";
        }
        if (code.equals("4")) {
            result = "";
        }
        if (code.equals("5")) {
            result = "";
        }
        if (code.equals("6")) {
            result = "";
        }
        if (code.equals("7")) {
            result = "";
        }
        if (code.equals("8")) {
            result = "";
        }
        if (code.equals("9")) {
            result = "";
        }
        if (code.equals("+")) {
            result = "";
        }
        if (code.equals("g")) {
            result = "";
        }
        if (code.equals("I")) {
            result = "";
        }
        if (code.equals("T")) {
            result = "";
        }
        if (code.equals("h")) {
            result = "";
        }
        if (code.equals("i")) {
            result = "";
        }
        if (code.equals("s")) {
            result = "";
        }
        if (code.equals("?")) {
            result = "";
        }

        return result;
    }
}