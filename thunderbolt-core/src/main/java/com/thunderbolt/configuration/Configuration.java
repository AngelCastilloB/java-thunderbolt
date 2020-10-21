/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.configuration;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/* IMPLEMENTATION ************************************************************/

/**
 * Provides access to the configuration file of the application.
 */
public class Configuration
{
    private static final Logger s_logger = LoggerFactory.getLogger(Configuration.class);

    private static short  m_nodePort          = 9567;
    private static int    m_minConnections    = 1;
    private static int    m_maxConnections    = 10;
    private static int    m_inactiveTime      = 3600000;
    private static int    m_heartbeat         = 1200000;
    private static String m_rpcUser           = "user";
    private static String m_rpcPassword       = "pass";
    private static short  m_rpcPort           = 3685;
    private static String m_walletPath        = "";
    private static double m_payTransactionFee = 0.0001;

    /**
     * Initializes the configuration file.
     *
     * @param path The path to the configuration file.
     */
    public static boolean initialize(String path) throws IOException
    {
        try (InputStream input = new FileInputStream(path))
        {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);
            s_logger.info("Config file '{}' loaded.", path);

            if (prop.containsKey("port"))
                m_nodePort = Short.parseShort(prop.getProperty("port"));

            if (prop.containsKey("min-connections"))
                m_minConnections = Integer.parseInt(prop.getProperty("min-connections"));

            if (prop.containsKey("max-connections"))
                m_maxConnections = Integer.parseInt(prop.getProperty("max-connections"));

            if (prop.containsKey("inactive-time"))
                m_inactiveTime = Integer.parseInt(prop.getProperty("inactive-time"));

            if (prop.containsKey("heartbeat"))
                m_heartbeat = Integer.parseInt(prop.getProperty("heartbeat"));

            if (prop.containsKey("rpc-user"))
                m_rpcUser = prop.getProperty("rpc-user");

            if (prop.containsKey("rpc-password"))
                m_rpcPassword = prop.getProperty("rpc-password");

            if (prop.containsKey("rpc-port"))
                m_rpcPort = Short.parseShort(prop.getProperty("rpc-port"));

            if (prop.containsKey("wallet"))
                m_walletPath = prop.getProperty("wallet");

            if (prop.containsKey("pay-tx-fee"))
                m_payTransactionFee = Double.parseDouble(prop.getProperty("pay-tx-fee"));
        }
        catch (FileNotFoundException e)
        {
            // Create file with default values.

            Properties props = new Properties();
            //Populating the properties file
            props.put("port", Short.toString(m_nodePort));
            props.put("min-connections", Integer.toString(m_minConnections));
            props.put("max-connections", Integer.toString(m_maxConnections));
            props.put("inactive-time", Integer.toString(m_inactiveTime));
            props.put("heartbeat", Integer.toString(m_heartbeat));
            props.put("rpc-user", m_rpcUser);
            props.put("rpc-password", m_rpcPassword);
            props.put("rpc-port", Short.toString(m_rpcPort));
            props.put("wallet", m_walletPath);
            props.put("pay-tx-fee", Convert.stripTrailingZeros(m_payTransactionFee));

            FileOutputStream outputStrem = new FileOutputStream(path);
            props.store(outputStrem, "Thunderbolt configuration file.");

            return false;
        }

        return true;
    }

    /**
     * Gets the port on which the node will listen for connections.
     *
     * @return The listening port.
     */
    public static short getNodePort()
    {
        return m_nodePort;
    }

    /**
     * Gets the minimum amount of peers we must connect during bootstrap.
     *
     * @return the minimum amount of peers needed for bootstrap.
     */
    public static int getNodeMinConnections()
    {
        return m_minConnections;
    }

    /**
     * Gets the maximum amount of peers we are allow to be connected at the same time.
     *
     * @return The maximum amount of peers.
     */
    public static int getNodeMaxConnections()
    {
        return m_maxConnections;
    }

    /**
     * Gets the time the peer is allowed to remain inactive before being disconnected.
     *
     * @return The time the peer is allowed to remain inactive.
     */
    public static int getPeerInactiveTime()
    {
        return m_inactiveTime;
    }

    /**
     * Gets the time in ms of every heartbeat signal. This signal is send so peers wont disconnect us.
     *
     * @return The time in ms of every heartbeat signal
     */
    public static int getPeerHeartbeat()
    {
        return m_heartbeat;
    }

    /**
     * The RPC username.
     *
     * @return The username.
     */
    public static String getRpcUser()
    {
        return m_rpcUser;
    }

    /**
     * The RPC password.
     *
     * @return The password.
     */
    public static String getRpcPassword()
    {
        return m_rpcPassword;
    }

    /**
     * The port on which the RPC server will listen for requests.
     *
     * @return The RPC port.
     */
    public static short getRpcPort()
    {
        return m_rpcPort;
    }

    /**
     * Gets the path to the wallet file.
     *
     * @return The path to the wallet file.
     */
    public static String getWalletPath()
    {
        return m_walletPath;
    }

    /**
     * Gets the transaction fee every time you send coins. This is per kilobyte.
     *
     * @return The transaction fee.
     */
    public static double getPayTransactionFee()
    {
        return m_payTransactionFee;
    }
}


