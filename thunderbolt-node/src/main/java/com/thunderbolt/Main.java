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

package com.thunderbolt;

/* IMPORTS *******************************************************************/

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.blockchain.StandardBlockchainCommitter;
import com.thunderbolt.blockchain.contracts.IBlockchainCommitter;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.network.Node;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.contracts.IPeerDiscoverer;
import com.thunderbolt.network.discovery.StandardPeerDiscoverer;
import com.thunderbolt.network.messages.ProtocolMessageFactory;
import com.thunderbolt.network.peers.PeerManager;
import com.thunderbolt.persistence.StandardPersistenceService;
import com.thunderbolt.persistence.contracts.IContiguousStorage;
import com.thunderbolt.persistence.contracts.IMetadataProvider;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.rpc.NodeHttpHandler;
import com.thunderbolt.transaction.*;
import com.thunderbolt.transaction.contracts.ITransactionValidator;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    // Constants
    static private final String USER_HOME_PATH    = System.getProperty("user.home");
    static private final String DATA_FOLDER_NAME  = ".thunderbolt";
    static private final Path   DEFAULT_PATH      = Paths.get(USER_HOME_PATH, DATA_FOLDER_NAME);
    static private final Path   BLOCKS_PATH       = Paths.get(DEFAULT_PATH.toString(), "blocks");
    static private final Path   REVERT_PATH       = Paths.get(DEFAULT_PATH.toString(), "reverts");
    static private final Path   METADATA_PATH     = Paths.get(DEFAULT_PATH.toString(), "metadata");
    static private final Path   ADDRESS_PATH      = Paths.get(DEFAULT_PATH.toString(), "peers");
    static private final Path   CONFIG_FILE_PATH  = Paths.get(DEFAULT_PATH.toString(), "thunderbolt.conf");
    static private final Path   WALLET_PATH       = Paths.get(USER_HOME_PATH, "wallet.bin");
    static private final String BLOCK_PATTERN     = "block%05d.bin";
    static private final String REVERT_PATTERN    = "revert%05d.bin";
    private static final int    RPC_THREAD_COUNT  = 2;
    private static final int    HTTP_CLOSE_DELAY  = 1000; //ms
    private static final int    EXIT_CODE_SUCCESS = 0; //ms

    // Static variables
    private static final Logger s_logger     = LoggerFactory.getLogger(Main.class);
    private static HttpServer   s_httpServer = null;

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException, StorageException
    {
        Configuration.initialize(CONFIG_FILE_PATH.toString());

        IPersistenceService           persistenceService   = createPersistenceService();
        MemoryTransactionsPool        memPool              = new MemoryTransactionsPool(persistenceService);
        ITransactionValidator         transactionValidator = new StandardTransactionValidator(persistenceService, NetworkParameters.mainNet());
        IBlockchainCommitter          committer            = new StandardBlockchainCommitter(persistenceService, memPool);
        Blockchain                    blockchain           = new Blockchain(NetworkParameters.mainNet(), transactionValidator, committer, persistenceService);
        IPeerDiscoverer               discoverer           = new StandardPeerDiscoverer();
        INetworkAddressPool           addressPool          = new LevelDbNetworkAddressPool(ADDRESS_PATH);

        blockchain.addOutputsUpdateListener(memPool);

        Path walletPath = Configuration.getWalletPath().isEmpty() ?
                WALLET_PATH : Paths.get(Configuration.getWalletPath());

        Wallet wallet = new Wallet(walletPath);
        wallet.initialize(persistenceService, memPool);

        memPool.addTransactionsChangedListener(wallet);
        blockchain.addBlockchainUpdateListener(wallet);
        blockchain.addOutputsUpdateListener(wallet);

        s_logger.info("Wallet file {}. Encrypted: {}, Unlocked: {}", walletPath, wallet.isEncrypted(), wallet.isUnlocked());
        s_logger.info("Address {}.", wallet.getAddress());
        ProtocolMessageFactory.initialize(NetworkParameters.mainNet(), persistenceService);

        PeerManager peerManager = new PeerManager(
                Configuration.getNodeMinConnections(),
                Configuration.getNodeMaxConnections(),
                Configuration.getPeerInactiveTime(),
                Configuration.getPeerHeartbeat(),
                discoverer,
                NetworkParameters.mainNet(),
                addressPool);

        if (!peerManager.start())
        {
            s_logger.debug("The peer manager could not be started. The node will shutdown");
            return;
        }

        Node node = new Node(NetworkParameters.mainNet(), blockchain, memPool, peerManager, persistenceService);

        startRpcService(node, wallet);
        node.run();

        s_httpServer.stop(HTTP_CLOSE_DELAY);
        s_logger.info("Node has stopped.");
        System.exit(EXIT_CODE_SUCCESS);
    }

    /**
     * Creates the persistence service.
     *
     * @return The newly created persistence service.
     */
    private static IPersistenceService createPersistenceService() throws StorageException
    {
        IContiguousStorage blockStorage    = new DiskContiguousStorage(BLOCKS_PATH, BLOCK_PATTERN);
        IContiguousStorage revertsStorage  = new DiskContiguousStorage(REVERT_PATH, REVERT_PATTERN);
        IMetadataProvider metadataProvider = new LevelDbMetadataProvider(METADATA_PATH);

        return new StandardPersistenceService(blockStorage, revertsStorage, metadataProvider);
    }

    /**
     * Starts the RPC service.
     *
     * @param node The node instance.
     * @param wallet The wallet instance.
     */
    private static void startRpcService(Node node, Wallet wallet)
    {
        new Thread(() ->
        {
            try
            {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(RPC_THREAD_COUNT);
                s_httpServer = HttpServer
                        .create(new InetSocketAddress("localhost", Configuration.getRpcPort()), 0);

                HttpContext context = s_httpServer.createContext("/", new NodeHttpHandler(node, wallet));

                context.setAuthenticator(new BasicAuthenticator("post")
                {
                    @Override
                    public boolean checkCredentials(String user, String pwd) {
                        return user.equals(Configuration.getRpcUser()) && pwd.equals(Configuration.getRpcPassword());
                    }
                });

                s_httpServer.setExecutor(threadPoolExecutor);

                s_logger.debug("RPC service listening on port: {}", Configuration.getRpcPort());
                s_httpServer.start();
            }
            catch (IOException exception)
            {
                s_logger.error("There was an error during the reading or writing of an RPC message.", exception);
                s_logger.error("The RPC server is down. Shutting down node.", exception);
                node.shutdown();
            }
        }).start();
    }
}