/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.thunderbolt.common;

/* IMPORTS ********************************************************************/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/* IMPLEMENTATION *************************************************************/

/**
 * Helper class for retrieving application related information.
 */
public class ApplicationInfo
{
    private static String s_version   = "0.0.0.0";
    private static String s_buildDate = "Unknown";
    private static String s_name      = "Unknown";
    private static String s_url       = "Unknown";

    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(ApplicationInfo.class);

    /**
     * Initializes the application information class.
     *
     * @param loader The application information.
     * @param file   The file with the application info.
     */
    public static void load(ClassLoader loader, String file)
    {
        InputStream in = loader.getResourceAsStream(file);
        Properties properties = new Properties();
        try
        {
            properties.load(in);

            s_version   = (String)properties.get("build.version");
            s_buildDate = (String)properties.get("build.date");
            s_name      = (String)properties.get("build.name");
            s_url       = (String)properties.get("build.url");
        }
        catch (IOException e)
        {
            s_logger.error("Could not load {}} file", file, e);
        }
    }

    /**
     * Gets the application version.
     *
     * @return The application version.
     */
    public static String getVersion()
    {
        return s_version;
    }

    /**
     * Gets the application build date.
     *
     * @return The application build date.
     */
    public static String getBuildDate()
    {
        return s_buildDate;
    }

    /**
     * Gets the application name.
     *
     * @return The application name.
     */
    public static String getName()
    {
        return s_name;
    }

    /**
     * Gets the application URL.
     *
     * @return The application URL.
     */
    public static String getUrl()
    {
        return s_url;
    }
}
