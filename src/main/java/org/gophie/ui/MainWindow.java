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
import org.gophie.config.SystemUtility;
import org.gophie.net.*;
import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.net.event.GopherClientEventListener;
import org.gophie.net.event.GopherError;
import org.gophie.ui.event.MessageViewListener;
import org.gophie.ui.event.NavigationInputListener;
import org.gophie.ui.event.PageMenuEventListener;
import org.gophie.ui.event.SearchInputListener;
import org.gophie.ui.util.GuiUtil;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;

@Slf4j
public class MainWindow implements NavigationInputListener, GopherClientEventListener, PageMenuEventListener {
    /* define the constants for the UI */
    public static final String APPLICATION_TITLE = "Gophie";
    public static final String NAVIGATIONBAR_BACKGROUND = "#248AC2";
    public static final String NAVIGATIONBAR_TEXTCOLOR = "#76bce3";
    public static final String NAVIGATIONBAR_TEXTHOVERCOLOR = "#ffffff";
    public static final String VIEW_BACKGROUND = "#1b1b1b";
    public static final String VIEW_TEXTCOLOR = "#e8e8e8";
    public static final String DEFAULT_GOPHERHOME = "gopher.floodgap.com";

    /* local network objects */
    private final GopherClient gopherClient;
    private final DownloadList downloadList;
    /* local ui elements */
    private final JFrame frame;
    private final PageView pageView;
    private final NavigationBar navigationBar;
    private final MessageView messageView;
    private final SearchInput searchInput;
    private final DownloadWindow downloadWindow;
    /* storage with history for browsing */
    private ArrayList<GopherPage> history = new ArrayList<>();
    private int historyPosition = -1;

    /**
     * Constructs this main window
     */
    public MainWindow() {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        /* create the instance of the client */
        gopherClient = new GopherClient();

        /* create the download list */
        downloadList = new DownloadList();

        /* create the download window */
        downloadWindow = new DownloadWindow(downloadList);

        /* create the main window */
        frame = new JFrame(APPLICATION_TITLE);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setIconImage(ConfigurationManager.getImage("icon.png"));
        GuiUtil.initGui(frame);

        /* MENU BAR */
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu optionsMenu = new JMenu("Options");
        menuBar.add(optionsMenu);

        JMenuItem changeThemeMenuItem = new JMenuItem("Change theme");
        changeThemeMenuItem.addActionListener(e -> changeTheme());
        optionsMenu.add(changeThemeMenuItem);

        JMenuItem increaseFontMenuItem = new JMenuItem("Increase font size");
        increaseFontMenuItem.addActionListener(e -> increaseFontSize());
        optionsMenu.add(increaseFontMenuItem);

        JMenuItem decreaseFontMenuItem = new JMenuItem("Decrease font size");
        decreaseFontMenuItem.addActionListener(e -> decreaseFontSize());
        optionsMenu.add(decreaseFontMenuItem);


        /* create the page view component object */
        pageView = new PageView(this);
        pageView.addListener(this);

        /* create the navigation bar */
        // TODO refactor to let theme handle color
        navigationBar = new NavigationBar(
                /* get the appearance configuration from the config file */
                configFile.getSetting("NAVIGATIONBAR_BACKGROUND", "Appearance", NAVIGATIONBAR_BACKGROUND),
                configFile.getSetting("NAVIGATIONBAR_TEXTCOLOR", "Appearance", NAVIGATIONBAR_TEXTCOLOR),
                configFile.getSetting("NAVIGATIONBAR_TEXTHOVERCOLOR", "Appearance", NAVIGATIONBAR_TEXTHOVERCOLOR)
        );
        
        /* set the gopher home as defined in the config
            or use the default one if none is defined */
        String gopherHome = configFile.getSetting("GOPHERHOME", "Navigation", DEFAULT_GOPHERHOME);
        navigationBar.setAddressText(gopherHome);

        /* attach listener to navigation bar */
        navigationBar.addListener(this);

        /* create the header bar, message view
            and search input component */
        JPanel headerBar = new JPanel();
        headerBar.setLayout(new BoxLayout(headerBar, BoxLayout.Y_AXIS));
        messageView = new MessageView();
        headerBar.add(messageView);
        searchInput = new SearchInput();
        headerBar.add(searchInput);

        /* set the content pane */
        Container contentPane = frame.getContentPane();
        contentPane.add(headerBar, BorderLayout.NORTH);
        contentPane.add(pageView, BorderLayout.CENTER);
        contentPane.add(navigationBar, BorderLayout.SOUTH);

        frame.setVisible(true);

        /* fetch the default gopher home */
        fetchGopherContent(gopherHome, GopherItemType.GOPHERMENU);
    }

    private void increaseFontSize() {
        GuiUtil.increaseGlobalFontSize();
    }

    private void decreaseFontSize() {
        GuiUtil.decreaseGlobalFontSize();
    }

    private void changeTheme() {
        String themeName = (String) JOptionPane.showInputDialog(
                null,
                "Choose theme:",
                "Change Theme",
                JOptionPane.QUESTION_MESSAGE,
                null,
                GuiUtil.getThemeNames(),
                GuiUtil.getCurrentTheme());
        if (themeName != null) {
            GuiUtil.updateTheme(themeName);
        }
    }

    /**
     * Shows this main window
     */
    public void show() {
        /* display the window */
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Updates the history with a new page
     *
     * @param page The page that was received
     */
    private void updateHistory(GopherPage page) {
        Boolean addToHistory = false;

        /* check if current position is at last page */
        if (historyPosition == history.size() - 1) {
            /* add this page to the history */
            if (!history.isEmpty()) {
                /* make sure this was not just a reload and the last
                    page in the history is not already ours */
                if (!history.get(history.size() - 1).getUrl().getUrlString()
                        .equals(page.getUrl().getUrlString())) {
                    /* just drop it in */
                    addToHistory = true;
                }
            } else {
                /* empty history, just drop in the page */
                addToHistory = true;
            }
        } else {
            /* user navigation inside history, check if the current
                page is at the position in history or if it is a 
                new page the user went to */
            if (!history.get(historyPosition).getUrl()
                    .getUrlString().equals(page.getUrl().getUrlString())) {
                /* it is a new page outside the history, keep the history
                    up until the current page and add this page as a new
                    branch to the history, eliminating the 
                    previous branch forward */
                ArrayList<GopherPage> updatedHistory = new ArrayList<>();
                for (int h = 0; h <= historyPosition; h++) {
                    updatedHistory.add(history.get(h));
                }

                /* update the history */
                history = updatedHistory;

                /* allow adding to history */
                addToHistory = true;
            }
        }

        /* reset navigation allowance */
        navigationBar.setNavigateBack(false);
        navigationBar.setNavigateForward(false);

        /* add to history, if allowed */
        if (addToHistory) {
            /* add to the stack of pages */
            history.add(page);

            /* update position to the top */
            historyPosition = history.size() - 1;

            /* disable forward */
            navigationBar.setNavigateForward(false);
            if (history.size() > 1) {
                /* allow back if more than just this page exist */
                navigationBar.setNavigateBack(true);
            }
        } else {
            /* if position is 0, there is nowhere to go back to */
            if (historyPosition > 0) {
                /* allow navigation back in history */
                navigationBar.setNavigateBack(true);
            }
            if (historyPosition < (history.size() - 1)) {
                /* if position is at the end, there is nowhere
                    to move forward to */
                navigationBar.setNavigateForward(true);
            }
        }
    }

    /**
     * Prompts user to choose on how to handle the
     * file and whether it should be saved only or
     * immediately downloaded to user home and
     * executed or opened when finished
     *
     * @param addressText the address (URL) to download
     * @param item        the item to download
     */
    public void confirmDownload(String addressText, GopherItem item) {
        /* binary files are handled by the download manager */
        String confirmText = "Download \"" + item.getFileName()
                + "\" from \"" + item.getHostName() + "\"?";
        String[] optionList = new String[]{"Open", "Save", "Dismiss"};
        messageView.showConfirm(confirmText, optionList, new MessageViewListener() {
            @Override
            public void optionSelected(int option) {
                if (option == 0) {
                    /* store file to download directory and open */
                    String targetFileName = ConfigurationManager.getDownloadPath() + item.getFileName();
                    downloadList.add(new DownloadItem(item, targetFileName, true));

                    /* hide the message view */
                    messageView.setVisible(false);
                }
                if (option == 1) {
                    /* initiate the download */
                    initiateDownload(item);

                    /* hide the message view */
                    messageView.setVisible(false);
                }

                /* hide the message view */
                messageView.setVisible(false);
            }
        });
    }

    /**
     * Prompts user to select the file destination
     * and immediately executes the download of the
     * file
     *
     * @param fileItem the item to download
     */
    public void initiateDownload(GopherItem fileItem) {
        /* let user select where to store the file */
        FileDialog fileDialog = new FileDialog(frame, "Download and save file", FileDialog.SAVE);
        fileDialog.setFile(fileItem.getFileNameWithForcedExt());
        fileDialog.setVisible(true);
        String targetFileName = fileDialog.getDirectory() + fileDialog.getFile();
        if (!targetFileName.equals("nullnull")) {
            /* pass url and target file to download manager */
            downloadList.add(new DownloadItem(fileItem, targetFileName, false));
        }
    }

    /**
     * Process a request to go to an address or URL
     *
     * @param addressText The text or URL of the address, the client will guess the correct URL
     *                    <p>
     *                    The expected content type of the content behind the address
     */
    @Override
    public void addressRequested(String addressText, GopherItem item) {
        /* check if this file is binary or not as
            binaries such as media or other files
            will be handled differently (e.g. downloaded) */
        if (item.isBinaryFile()) {
            /* binary files are handled by the download manager */
            confirmDownload(addressText, item);
        } else {
            /* this is not a binary file, try to handle and render */
            switch (item.getItemType()) {
                case FULLTEXT_SEARCH:
                    /* show the search interface */
                    searchInput.performSearch(item.getUserDisplayString(), new SearchInputListener() {
                        @Override
                        public void searchRequested(String text) {
                            /* execute search through gopher */
                            String searchQueryText = addressText + "\t" + text;
                            fetchGopherContent(searchQueryText, GopherItemType.GOPHERMENU);
                        }
                    });
                    break;
                case CCSCO_NAMESERVER:
                    /* CCSO is not part of the Gopher protocol, but its very own
                        protocol and apart from floodgap.com's CCSO server there
                        is hardly any server to test interaction with. The CCSO
                        protocol can also be considered quite simple. A CCSO client
                        would be a software of its own, but sources are even fewer
                        than Gopher servers out there. Hence, Gophie allows the
                        user to use CCSO servers throgh their Telnet client. */
                    openTelnetSession(item.getHostName(), item.getPortNumber());
                    break;
                case TELNET:
                    /* handle telnet session requests */
                    openTelnetSession(item.getHostName(), item.getPortNumber());
                    break;
                case TELNET3270:
                    /* handle telnet 3270 session requests */
                    openTelnetSession(item.getHostName(), item.getPortNumber());
                    break;
                default:
                    /* check what type of link was requested and execute
                        the appropriate external application or use the
                        default approach for gopher content */
                    if (addressText.startsWith("https://")
                            || addressText.startsWith("http://")) {
                        /* this is the World Wide Web using HTTP or HTTPS, 
                            so try to open the systems browser so that the
                            user can enjoy bloated javascript based html
                            content with the fine-art of pop-up advertising
                            and animated display banners */
                        openWebContent(addressText, item.getItemType());
                    } else if (addressText.startsWith("mailto:")) {
                        /* this is a mailto link */
                        openEmailClient(addressText.replace("mailto:", ""));
                    } else {
                        /* just fetch as regular gopher content */
                        fetchGopherContent((new GopherUrl(addressText)).getUrlString(), item.getItemType());
                    }
                    break;
            }
        }
    }

    /**
     * Opens the clients email address
     *
     * @param emailAddress the email address to send to
     */
    private void openEmailClient(String emailAddress) {
        String confirmText = "Do you want to send an e-mail to \"" + emailAddress + "\"?";
        String[] optionList = new String[]{"Create new e-mail", "Dismiss"};
        messageView.showConfirm(confirmText, optionList, new MessageViewListener() {
            @Override
            public void optionSelected(int option) {
                if (option == 0) {
                    /* launch the system email client */
                    if (Desktop.isDesktopSupported()
                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            /* launch the mailto handler of the system */
                            Desktop.getDesktop().browse(new URI("mailto:" + emailAddress));
                        } catch (Exception ex) {
                            /* Error: cannot open email client */
                            log.error("Unable to open system's email client: {}", ex.getMessage());
                        }
                    }
                    /* hide the message view */
                    messageView.setVisible(false);
                } else {
                    /* hide the message view */
                    messageView.setVisible(false);
                }
            }
        });
    }

    /**
     * Prompts the user and opens the systems telnet client
     *
     * @param hostName   host name of the telnet server
     * @param portNumber port number of the telnet server
     */
    private void openTelnetSession(String hostName, int portNumber) {
        String confirmText = "Open a Telnet session with \"" + hostName + ":" + portNumber + "\"?";
        String[] optionList = new String[]{"Open Telnet", "Dismiss"};
        messageView.showConfirm(confirmText, optionList, new MessageViewListener() {
            @Override
            public void optionSelected(int option) {
                if (option == 0) {
                    /* launch the system WWW browser */
                    if (Desktop.isDesktopSupported()
                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            /* launch the systems telnet client by creating
                                a telnet URI and calling the systems protocol handler */
                            String telnetUri = "telnet://" + hostName + ":" + portNumber;
                            Desktop.getDesktop().browse(new URI(telnetUri));
                        } catch (Exception ex) {
                            /* Error: cannot open telnet client */
                            log.error("Unable to open system's telnet client: {}", ex.getMessage());
                        }
                    }
                    /* hide the message view */
                    messageView.setVisible(false);
                } else {
                    /* hide the message view */
                    messageView.setVisible(false);
                }
            }
        });
    }

    /**
     * Ask user to open web content through http
     *
     * @param addressText The actual address requested
     * @param contentType The actual content type of the content
     */
    private void openWebContent(String addressText, GopherItemType contentType) {
        String confirmText = "Open \"" + addressText + "\" with your web browser?";
        String[] optionList = new String[]{"Open Website", "Dismiss"};
        messageView.showConfirm(confirmText, optionList, new MessageViewListener() {
            @Override
            public void optionSelected(int option) {
                if (option == 0) {
                    /* launch the system WWW browser */
                    if (Desktop.isDesktopSupported()
                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            /* launch the systems WWW browser */
                            Desktop.getDesktop().browse(new URI(addressText));
                        } catch (Exception ex) {
                            /* Error: cannot enjoy bloated javascript 
                                    stuffed World Wide Web pages! */
                            log.error("Unable to open system's world wide web browser: {}", ex.getMessage());
                        }
                    }
                    /* hide the message view */
                    messageView.setVisible(false);
                } else {
                    /* hide the message view */
                    messageView.setVisible(false);
                }
            }
        });
    }

    /**
     * Fetches gopher menu or text content
     *
     * @param addressText The address to fetch content from
     * @param contentType The actual content type requested
     */
    private void fetchGopherContent(String addressText, GopherItemType contentType) {
        /* this is default gopher content */
        /* activate the load indicator in the address bar */
        navigationBar.setIsLoading(true);

        /* update the navigation bar with the new address */
        String address = addressText;

        /* check if selector prefixes are enabled */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String prefixEnabled = configFile.getSetting("SELECTOR_PREFIX_ENABLED", "Navigation", "yes");
        if (prefixEnabled.equals("yes")) {
            /* create the gopher url object for the address */
            GopherUrl prefixUrl = new GopherUrl(addressText);
            prefixUrl.setTypePrefix(GopherItem.getTypeCode(contentType));

            /* set the address to the url with the prefix */
            address = prefixUrl.getUrlString(true);
        }

        /* update the navigation bar with the new address */
        navigationBar.setAddressText(address);

        try {
            /* try to execute the thread */
            gopherClient.fetchAsync(addressText, contentType, this);
        } catch (Exception ex) {
            /* might throw an ex when thread is interrupted */
            log.error("Exception while fetching async: {}", ex.getMessage());
        }
    }

    /**
     * Navigates backwards in the history
     */
    @Override
    public void backwardRequested() {
        /* set the new history position */
        if (historyPosition > 0) {
            historyPosition--;

            /* get the new page from history */
            pageLoaded(history.get(historyPosition));

            /* update the history */
            updateHistory(history.get(historyPosition));
        }
    }

    @Override
    public void forwardRequested() {
        /* set the new history position */
        if (historyPosition < (history.size() - 1)) {
            historyPosition++;

            /* get the new page from history */
            pageLoaded(history.get(historyPosition));

            /* update the history */
            updateHistory(history.get(historyPosition));
        }
    }

    /**
     * Refreshes the current page
     */
    @Override
    public void refreshRequested() {
        /* get the current gopher page to reload it */
        GopherPage currentPage = history.get(historyPosition);

        /* reload practically means just requesting this page again */
        fetchGopherContent(currentPage.getUrl().getUrlString(), currentPage.getContentType());
    }

    /**
     * Stops the current page load
     */
    @Override
    public void stopRequested() {
        /* cancel any current operation */
        gopherClient.cancelFetch();

        /* notify the local handler about cancellation by the user */
        pageLoadFailed(GopherError.USER_CANCELLED, null);
    }

    /**
     * Handles page load events from the listener
     *
     * @param result The gopher page that was received
     */
    @Override
    public void pageLoaded(GopherPage result) {
        /* set the window title to the url of this page */
        frame.setTitle(result.getUrl().getUrlString()
                + " (" + SystemUtility.getFileSizeString(result.getByteArray().length) + ")"
                + " - " + APPLICATION_TITLE);

        /* update the address text with the loaded page */
        String address = result.getUrl().getUrlString();

        /* check if selector prefixes are enabled */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String prefixEnabled = configFile.getSetting("SELECTOR_PREFIX_ENABLED", "Navigation", "yes");
        if (prefixEnabled.equals("yes")) {
            /* create the gopher url object for the address */
            GopherUrl prefixUrl = result.getUrl();
            prefixUrl.setTypePrefix(GopherItem.getTypeCode(result.getContentType()));

            /* set the address to the url with the prefix */
            address = prefixUrl.getUrlString(true);
        }

        /* set the navigation bar to the new address */
        navigationBar.setAddressText(address);


        /* detect the content type and determine how the handle it */
        if (result.getContentType() == GopherItemType.GOPHERMENU) {
            /* this is a gopher menu hence it is rendered like
                one including highlighting of links and 
                the menu icons for the various item types */
            pageView.showGopherPage(result);
        } else {
            /* this is plain content, so render it
                appropriately and let the view decide
                on how to handle the content */
            pageView.showGopherContent(result);
        }

        /* update the history */
        updateHistory(result);

        /* reset the loading indicators */
        navigationBar.setIsLoading(false);
    }

    /**
     * Reports failed page load
     */
    @Override
    public void pageLoadFailed(GopherError error, GopherUrl url) {
        /* show message for connection timeout */
        if (error == GopherError.CONNECT_FAILED) {
            if (url != null) {
                messageView.showInfo("Connection refused: " + url.getHost());
            }
        }

        /* show message for connection timeout */
        if (error == GopherError.CONNECTION_TIMEOUT) {
            if (url != null) {
                messageView.showInfo("Connection timed out: " + url.getHost());
            }
        }

        /* show DNS or host not found error */
        if (error == GopherError.HOST_UNKNOWN) {
            if (url != null) {
                messageView.showInfo("Server not found: " + url.getHost());
            }
        }

        /* show some information about an exception */
        if (error == GopherError.EXCEPTION) {
            messageView.showInfo("Ouchn, an unknown error occured.");
        }

        /* output some base information to the console */
        log.error("Failed to load gopher page: {}", error.toString());

        /* reset the navigation bar status */
        navigationBar.setIsLoading(false);
    }

    /**
     * Report progress on the page loading
     */
    @Override
    public void progress(GopherUrl url, long byteCount) {
        /* report the download size in the title bar */
        frame.setTitle(url.getUrlString()
                + " (" + SystemUtility.getFileSizeString(byteCount) + ")"
                + " - " + APPLICATION_TITLE);
    }

    /**
     * Toggles the download window
     */
    @Override
    public void showDownloadRequested() {
        if (downloadWindow.isVisible()) {
            downloadWindow.hide();
        } else {
            downloadWindow.show(frame);
        }
    }

    /**
     * Updates the gopher home with the provided url
     */
    @Override
    public void setHomeGopherRequested(String url) {
        /* set the gopher home to the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        configFile.setSetting("GOPHERHOME", url, "Navigation");
        configFile.save();
    }

    /**
     * initiates the download of the requested file
     */
    @Override
    public void itemDownloadRequested(GopherItem item) {
        initiateDownload(item);
    }

    /**
     * Saves the current page to file
     */
    @Override
    public void pageSaveRequested(GopherPage page) {
        /* let user select where to store the file */
        FileDialog fileDialog = new FileDialog(frame, "Save current file", FileDialog.SAVE);
        fileDialog.setFile(page.getFileName());
        fileDialog.setVisible(true);
        String targetFileName = fileDialog.getDirectory() + fileDialog.getFile();
        if (!targetFileName.equals(null)
                && !targetFileName.equals("nullnull")) {
            /* pass url and target file to download manager */
            page.saveAsFile(targetFileName);
        }
    }

    /**
     * Selects all text on the current page
     */
    @Override
    public void selectAllTextRequested() {
        /* hand that one back to the page view */
        pageView.selectAllText();
    }

    /**
     * Sends the user to his gopher home
     */
    @Override
    public void homeGopherRequested() {
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String homeGopherUrl = configFile.getSetting("GOPHERHOME", "Navigation", DEFAULT_GOPHERHOME);
        fetchGopherContent(homeGopherUrl, GopherItemType.GOPHERMENU);
        navigationBar.setAddressText(homeGopherUrl);
    }

    /**
     * Handles item mismatch events when the content detection
     * during the page loading process detects a different file
     * type than the one requested.
     */
    @Override
    public void pageLoadItemMismatch(GopherItemType requested, GopherItemType detected, GopherUrl url) {
        /* reset the navigation bar status */
        navigationBar.setIsLoading(false);

        /* binary files are handled by the download manager */
        confirmDownload(url.getUrlString(), (new GopherItem(detected, url)));
    }
}