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

package com.thunderbolt.network.discovery;

/* IMPORTS *******************************************************************/

import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.network.contracts.IPeerDiscoverer;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * This discoverer will get the peers from a list of hardcoded addresses.
 */
public class StandardPeerDiscoverer implements IPeerDiscoverer
{
    /**
     * Gets a list of the discovered peers.
     *
     * @return The list of peers.
     */
    public List<InetSocketAddress> getPeers()
    {
        List<InetSocketAddress> addresses = new LinkedList<>();

        addresses.add(new InetSocketAddress("35.206.239.89", Configuration.getNodePort()));

        return addresses;
    }
}
