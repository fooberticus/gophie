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

package org.gophie.config;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * This class parses *.INI configuration files
 * as defined in: https://en.wikipedia.org/wiki/INI_file
 */
@Slf4j
public class ConfigFile {
    /* hashmap with sections and settings */
    HashMap<String, HashMap<String, String>> config;
    /* full file name of this config file */
    private String fileName = "";

    /**
     * Creates a new instance of an *.INI config file
     *
     * @param configFileName the full file name of the config file
     */
    public ConfigFile(String configFileName) {
        config = new HashMap<>();
        fileName = configFileName;
        parse();
    }

    /**
     * Gets a setting from the config map
     *
     * @param name         Name of the setting
     * @param section      Section the setting is in
     * @param defaultValue Default value to return
     * @return Returns the setting value or the default
     * value if the setting or its section is not present
     */
    public String getSetting(String name, String section, String defaultValue) {
        String result = defaultValue;

        if (config.containsKey(section)) {
            if (config.get(section).containsKey(name)) {
                result = config.get(section).get(name);
            }
        }

        return result;
    }

    /**
     * Adds a setting to the current config map
     *
     * @param name    Name of the setting
     * @param value   Value of the setting
     * @param section Name of the section
     */
    public void setSetting(String name, String value, String section) {
        /* avoid adding empty values to the config map */
        if (!section.isEmpty() && !name.isEmpty() && !value.isEmpty()) {
            /* create a new hashmap with settings for this section */
            HashMap<String, String> settingMap = new HashMap<String, String>();

            if (config.containsKey(section)) {
                /* get the current setting map for this section */
                settingMap = config.get(section);
            }

            /* put name and value to the setting map */
            settingMap.put(name, value);
            config.put(section, settingMap);
        }
    }

    /**
     * Create the ini file text content
     * to be written into the ini file
     *
     * @return The text content of the ini file as string
     */
    private String getTextContent() {
        StringBuilder result = new StringBuilder();

        /* iterate through the hash map and build the ini file content */
        for (Entry<String, HashMap<String, String>> pair : config.entrySet()) {
            /* get the current entry */
            /* set the section header first */
            if (!result.isEmpty()) {
                result.append("\n");
            }
            result.append("[").append(pair.getKey()).append("]\n");

            /* iterate through all the settings values */
            for (Entry<String, String> settingEntry : pair.getValue().entrySet()) {
                /* get the current setting for this section */
                result.append(settingEntry.getKey()).append(" = ").append(settingEntry.getValue()).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Saves this config ini file to disk
     */
    public void save() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(getTextContent());
            writer.close();
        } catch (Exception ex) {
            log.error("Failed to write config file: {}", ex.getMessage());
        }
    }

    /**
     * Parse the configuration file
     */
    private void parse() {
        try {
            /* make sure that config file exists */
            if (Files.exists(Paths.get(fileName))) {
                /* read all lines from the defined file */
                List<String> lineList = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());

                /* go through each line */
                String configSection = "NONE";
                for (String line : lineList) {
                    /* check the type of line */
                    String value = line.trim();
                    if (value.length() > 0) {
                        /* ignore comments */
                        if (!value.startsWith(";")) {
                            /* check if this is a section */
                            if (value.startsWith("[") && value.endsWith("]")) {
                                /* this is a section, track it to assign values */
                                configSection = value.substring(1, value.length() - 1);
                            }

                            /* check if this is a value assignment */
                            if (value.indexOf("=") > 0) {
                                String[] setting = value.split("=");
                                if (setting.length == 2) {
                                    /* apply the setting to the config */
                                    setSetting(setting[0].trim(), setting[1].trim(), configSection);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            /* failed to parse config file */
            log.error("Failed to open and parse file ({}): {}", fileName, ex.getMessage());
        }
    }
}