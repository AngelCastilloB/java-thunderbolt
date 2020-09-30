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

package com.thunderbolt.network.contracts;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.network.messages.ProtocolMessage;

/* IMPLEMENTATION ************************************************************/

/**
 * Another node connected to use through the blockchain network.
 */
public interface IPeer
{
    /**
     * Test whether that address is reachable. Best effort is made by the implementation to try to reach the host, but
     * firewalls and server configuration may block requests resulting in a unreachable status while some specific
     * ports may be accessible.
     *
     * @param timeout the time, in milliseconds, before the call aborts.
     *
     * @return a {@code boolean} indicating if the address is reachable.
     */
    boolean isReachable(int timeout);

    /**
     * Gets the ban score for this peer connection.
     *
     * @return The ban score.
     */
    int getBanScore();

    /**
     * Adds to the ban score of this peer. The higher the ban score, the more likely the peer will be
     * disconnected.
     *
     * @param score The ban score to be added.
     *
     * @return The new ban score.
     */
    int addBanScore(int score);

    /**
     * Sets the ban score of this peer.
     *
     * @param score The ban score.
     */
    void setBanScore(int score);

    /**
     * Gets whether this peer connected to us, or from a peer we connected to during bootstrap.
     *
     * @return True if the peer connected to use; otherwise; false.
     */
    boolean isClient();

    /**
     * Gets whether the connection is established or not.
     *
     * @return True if connected; otherwise; false.
     */
    boolean isConnected();

    /**
     * Gets whether there are new messages to be read in this connection object.
     *
     * @return True if the peer has new messages; otherwise; false.
     */
    boolean hasMessage();

    /**
     * Gets a new message from the peer.
     *
     * @return The new message.
     */
    ProtocolMessage getMessage();

    /**
     * Sends a new message to the peer.
     *
     * @param message The message to be send.
     */
    void sendMessage(ProtocolMessage message);

    /**
     * Gets the time elapsed since the last message from this peer was received.
     *
     * @return The time elapsed since the last message arrived from this peer.
     */
    TimeSpan getInactiveTime();
}
