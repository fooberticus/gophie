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

public class GopherUrl {
    private int port = 70;
    private String host;
    private String selector;

    /**
     * constructs the object and parses the url
     *
     * @param url the url to parse as string
     */
    public GopherUrl(String url) {
        host = url;

        /* check if the url contains the protocol specifier */
        if (host.startsWith("gopher://")) {
            host = host.substring(9);
        }

        /* check if a selector was provided */
        if (host.indexOf("/") > 0) {
            selector = host.substring(host.indexOf("/"));
            host = host.substring(0, host.indexOf("/"));
        } else {
            /* no selector present, set the default to empty string */
            selector = "";
        }

        /* check if a port number was provided */
        if (host.indexOf(":") > 0) {
            /* remove port number from host name */
            String[] valueList = host.split(":");
            host = valueList[0];

            /* set the port number separately */
            port = Integer.parseInt(valueList[1]);
        }
    }

    /**
     * Returns the port number of this address
     *
     * @return the port number as integer
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the host name of this url
     *
     * @return host name as string
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the selector of this url
     *
     * @return the selector as string
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Returns the type prefix from the url, if any
     *
     * @return type prefix (gopher item type code) as string
     */
    public String getTypePrefix() {
        String result = null;

        if (selector.length() >= 3) {
            if (selector.charAt(0) == '/' && selector.charAt(2) == '/') {
                String itemTypeCode = selector.substring(1, 2);
                if (itemTypeCode.matches("[0-9+gIThis?]")) {
                    result = itemTypeCode;
                }
            }
        }

        return result;
    }

    /**
     * Sets or overwrites the type prefix for this url
     *
     * @param prefix single-character type prefix as string
     */
    public void setTypePrefix(String prefix) {
        /* check if a type prefix is present already */
        if (hasTypePrefix()) {
            /* replace the existing type prefix with the new one */
            if (selector.length() > 3) {
                if (selector.charAt(3) == '/') {
                    selector = "/" + prefix + selector.substring(3);
                } else {
                    selector = "/" + prefix + "/" + selector.substring(3);
                }
            } else {
                /* only the prefix is in the selector, replace it */
                selector = "/" + prefix + "/";
            }
        } else {
            if (!selector.isEmpty()) {
                /* just add the type prefix to the selector */
                if (selector.charAt(0) == '/') {
                    selector = "/" + prefix + selector;
                } else {
                    selector = "/" + prefix + "/" + selector;
                }
            } else {
                /* just set the prefix as the selector */
                selector = "/" + prefix + "/";
            }
        }
    }

    /**
     * Determines whether the url's selector
     * has a type prefix for the gopher item
     *
     * @return
     */
    public boolean hasTypePrefix() {
        boolean result = getTypePrefix() != null;

        return result;
    }

    /**
     * Returns the url string for this url
     * without type prefix, if present
     *
     * @return the url string of the url
     */
    public String getUrlString() {
        return getUrlString(false);
    }

    /**
     * Returns the url string for this url
     *
     * @param includeTypePrefix when true will include the type prefix in the
     *                          url, if any is available. If none is available,
     *                          this parameter has no effect.
     * @return the url as string
     */
    public String getUrlString(boolean includeTypePrefix) {
        String result = host;

        if (port != 70) {
            result += ":" + port;
        }

        /* strip the item type prefix as it is just
            for presentation and technically not part
            of the url itself */
        String selectorValue = selector;
        if (hasTypePrefix()) {
            if (!includeTypePrefix) {
                /* remove the type prefix if requested */
                selectorValue = selectorValue.substring(3);
            }
        }

        if (!selectorValue.isEmpty()) {
            if (selectorValue.startsWith("/")) {
                result += selectorValue;
            } else {
                result += "/" + selectorValue;
            }
        }

        return result;
    }
}