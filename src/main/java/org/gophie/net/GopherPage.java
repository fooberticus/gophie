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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gophie.config.ConfigurationManager;
import org.gophie.net.GopherItem.GopherItemType;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;

/**
 * A GopherMenu page object that contains all information
 * and Gopher items provided in the underlying Gopher Menu
 */
@Slf4j
public class GopherPage {
    /* defines the default charset */
    private static final String GOPHERPAGE_DEFAULT_CHARSET = "UTF-8";

    /**
     * -- SETTER --
     *  Sets the source code (gophermap) of this gopher page
     *
     * @param value The text value as supplied by the server
     */
    /* local variables */
    @Setter
    private byte[] sourceCode;
    /**
     * -- GETTER --
     *  Returns the GopherUrl object for this page
     *
     * @return GopherUrl object with url of this page
     */
    @Getter
    private final GopherUrl url;
    /**
     * -- GETTER --
     *  Returns an array list with all gopher items of this page
     *
     * @return ArrayList with all GopherItem objects
     */
    @Getter
    private final ArrayList<GopherItem> itemList;
    /**
     * -- GETTER --
     *  Returns the content type of this page
     *
     * @return The content type as gopher item type
     */
    @Getter
    private GopherItemType contentType = GopherItemType.UNKNOWN;

    /**
     * Constructs the GopherPage object and if it is
     * a gopher menu or unknown it tries to parse it
     * as a gopher menu. If that fails, it will try
     * to evaluate the content type and store the
     * source code.
     *
     * @param gopherPageSourceCode Source code or content of the gopher page
     * @param gopherContentType    The estimated content type of the gopher page
     * @param gopherPageUrl        The URL of the gopher page
     */
    public GopherPage(byte[] gopherPageSourceCode, GopherItemType gopherContentType, GopherUrl gopherPageUrl) {
        sourceCode = gopherPageSourceCode;
        url = gopherPageUrl;
        itemList = new ArrayList<>();

        if (gopherContentType == GopherItemType.GOPHERMENU
                || gopherContentType == GopherItemType.UNKNOWN) {
            /* try to parse it as a gopher menu */
            try {
                /* execute the parse process */
                parse();

                /* parsing succeeded, define as gopher menu */
                contentType = GopherItemType.GOPHERMENU;
            } catch (Exception ex) {
                /* output the parser exception */
                log.error("Failed to parse gophermenu: {}", ex.getMessage(), ex);

                /* parsing failed for whatever, define as text */
                contentType = GopherItemType.TEXTFILE;
            }
        } else {
            /* set the supplied content type */
            contentType = gopherContentType;
        }
    }

    /**
     * Returns the source code in base64 encoded format
     * which can be used to display images in the view
     *
     * @return String with base64 encoded data of the source code
     */
    public String getBase64() {
        return Base64.getEncoder().encodeToString(sourceCode);
    }

    /**
     * Returns the raw bytes of the data received
     *
     * @return Byte array with the raw gopher page data
     */
    public byte[] getByteArray() {
        return sourceCode;
    }

    /**
     * Returns the source code (gophermap) of this page
     *
     * @return The gophermap content as a String
     */
    public String getSourceCode() {
        try {
            return new String(sourceCode, ConfigurationManager.getConfigFile()
                    .getSetting("DEFAULT_CHARSET", "Network", GOPHERPAGE_DEFAULT_CHARSET));
        } catch (Exception ex) {
            /* drop a quick info on the console when decoding fails */
            log.error("Failed to decode bytes of Gopher Page: {}", ex.getMessage());
            return "";
        }
    }

    /**
     * Returns all text content of this page
     *
     * @return All text content of this page as string
     */
    public String getTextContent() {
        String result = "";

        if (itemList.size() > 0) {
            /* get the actual text from all gopher items */
            for (GopherItem item : itemList) {
                result += item.getUserDisplayString() + "\n";
            }
        } else {
            /* just return the source code and remove the
                line termination from the gopher server */
            return getSourceCode().replace("\r\n.\r\n", "");
        }

        return result;
    }

    /**
     * Saves the contents of this
     * page to the defined file
     *
     * @param fileName The file to store content in
     * @return truen when successful, otherwise false
     */
    public Boolean saveAsFile(String fileName) {
        boolean result = false;

        try {
            /* store this page content to file */
            FileOutputStream fileOutput = new FileOutputStream(fileName);
            fileOutput.write(getByteArray());
            fileOutput.close();
            result = true;
        } catch (Exception ex) {
            /* output the exception info when file storage failed */
            log.error("Failed to save page as file: {}", ex.getMessage());
        }

        return result;
    }

    /**
     * Returns the filename for this page
     *
     * @return The file name as string with extension
     */
    public String getFileName() {
        String result = getUrl().getUrlString();

        /* check if the file has a file name */
        if (result.lastIndexOf("/") > 0) {
            /* use the file name provided in the url */
            result = result.substring(result.lastIndexOf("/") + 1);
        } else {
            /* just call the file index when none exists */
            result = "index";
        }

        /* check if the file has an extension */
        if (result.lastIndexOf(".") == -1) {
            result += "." + GopherItem.getDefaultFileExt(getContentType());
        }

        return result;
    }

    /**
     * parses the local source code into components
     */
    private void parse() {
        String[] itemSourceList = getSourceCode().split("\n");
        for (String itemSource : itemSourceList) {
            if (!itemSource.isEmpty() && !itemSource.equals(".")) {
                itemList.add(new GopherItem(itemSource));
            }
        }
    }
}