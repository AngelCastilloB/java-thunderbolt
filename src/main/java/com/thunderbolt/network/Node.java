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

package com.thunderbolt.network;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.network.contracts.IPeer;
import com.thunderbolt.network.contracts.IPeerManager;
import com.thunderbolt.network.messages.MessageType;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.VersionPayload;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;

/* IMPLEMENTATION ************************************************************/

/**
 * Network node. Handles all the messages exchanges between this instance and the peers.
 */
public class Node
{
    private static final int MAIN_LOOP_DELAY = 100; // ms

    private static final Logger s_logger = LoggerFactory.getLogger(Node.class);

    private final NetworkParameters        m_params;
    private final Blockchain               m_blockchain;
    private boolean                        m_isRunning;
    private final ITransactionsPoolService m_memPool;
    private final IPeerManager             m_peerManager;

    /**
     * Initializes a new instance of the Node class.
     *
     * @param params The network parameters.
     * @param blockchain The blockchain instance.
     * @param transactionsPoolService The transaction pool service.
     * @param peerManager The peer manager.
     */
    public Node(NetworkParameters params,
                Blockchain blockchain,
                ITransactionsPoolService transactionsPoolService,
                IPeerManager peerManager)
    {
        m_params = params;
        m_blockchain = blockchain;
        m_memPool = transactionsPoolService;
        m_peerManager = peerManager;
    }

    /**
     * Shuts down the node
     */
    public void shutdown()
    {
        if (!m_isRunning)
            return;

        m_isRunning = false;

        s_logger.debug("Please wait while the node shuts down");
        m_peerManager.stop();
    }

    /**
     * Tries to connect to seed peers.
     */
    public void run()
    {
        if (!m_peerManager.start())
        {
            s_logger.debug("The peer manager could not be started. The node will shutdown");
            return;
        }

        m_isRunning = true;

        while (m_isRunning)
        {
            Iterator<IPeer> it = m_peerManager.getPeers();
            while (it.hasNext())
            {
                IPeer peer = it.next();

                while (peer.hasMessage())
                {
                    ProtocolMessage message = peer.getMessage();
                    process(message, peer);
                    // Do something.

                    // Punish peer.
                    //standardPeer.addBanScore(100);

                    // Send new message.
                    //standardPeer.sendMessage(new ProtocolMessage(NetworkParameters.mainNet().getPacketMagic()));
                }

                try
                {
                    Thread.sleep(MAIN_LOOP_DELAY);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Process the incoming messages from the peers.
     *
     * @param message The message to be processed.
     * @param peer The peer.
     */
    public void process(ProtocolMessage message, IPeer peer)
    {
        switch (message.getMessageType())
        {
            case Ping:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                ProtocolMessage response = new ProtocolMessage(m_params.getPacketMagic());
                response.setNonce(message.getNonce());

                peer.sendMessage(response);
                break;
            case Pong:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                if (!peer.isPongPending())
                {
                    peer.addBanScore(1);
                    return;
                }
                break;
            case Version:
                if (peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                VersionPayload payload = new VersionPayload(message.getPayload());

                if (payload.getVersion() == m_params.getProtocol())
                {
                    if (!peer.isClient())
                    {
                        s_logger.debug("Sending version message to peer {}", peer);
                        ProtocolMessage version = new ProtocolMessage(m_params.getPacketMagic());
                        version.setMessageType(MessageType.Version);

                        VersionPayload versionPayload = new VersionPayload(
                                m_params.getProtocol(),
                                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), 0); // TODO: Where do we put this?
                        version.setPayload(versionPayload);

                        peer.sendMessage(version);
                    }
                    else
                    {
                        ProtocolMessage verack = new ProtocolMessage(m_params.getPacketMagic());
                        verack.setMessageType(MessageType.Verack);

                        peer.setProtocolVersion(payload.getVersion());
                        peer.sendMessage(verack);
                        s_logger.debug("Sending verack message to peer {}", peer);
                    }
                }
                else
                {
                    s_logger.debug(
                            "Peer {} disconnected since version is incompatible. Ours {}, his {}.",
                            peer,
                            payload.getVersion(),
                            m_params.getProtocol());

                    peer.disconnect();
                }
                break;
            case Verack:
                if (peer.getProtocolVersion() == 0 && peer.isClient())
                {
                    peer.addBanScore(1);
                    return;
                }

                s_logger.debug("Handshake with Peer {} successful.", peer);
                peer.setClearedHandshake(true);
                break;
            default:
        }
    }
}
