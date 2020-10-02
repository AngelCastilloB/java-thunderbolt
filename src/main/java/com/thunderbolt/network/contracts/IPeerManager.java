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

import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;

import java.net.Socket;
import java.util.Iterator;

/* IMPLEMENTATION ************************************************************/

/**
 * A peer manager handles the life cycle of the peer as well as the connection, disconnection and message relay.
 */
public interface IPeerManager
{
    /**
     * Gets an iterator to the first peer on the manager.
     *
     * @return An iterator to the first peer of the manager.
     */
    Iterator<IPeer> getPeers();

    /**
     * Starts the manager.
     *
     * @return true if the manager cold be started; otherwise; false.
     */
    boolean start();

    /**
     * Stops the manager.
     */
    void stop();

    /**
     * Gets the amount of peers currently in the pool.
     *
     * @return The amount of peers connected to this node.
     */
    int peerCount();

    /**
     * Adds a peer to the manager.
     *
     * @param params      The network parameters.
     * @param peerSocket  The peer socket.
     * @param isInbound   Whether this connection came from a peer connecting to us, or from a peer we connected to
     *                    during bootstrap.
     *
     * @return The newly added peer.
     */
    IPeer add(NetworkParameters params, Socket peerSocket, boolean isInbound);

    /**
     * Removes a peer from this node.
     *
     * Note: If a peer connection is closed, the peer will be dropped automatically by this manager.
     *
     * @param peer The peer connected to this node.
     */
    void remove(IPeer peer);

    /**
     * Gets a reference fo the address pool instance.
     *
     * @return The pool instance.
     */
    INetworkAddressPool getAddressPool();
}
