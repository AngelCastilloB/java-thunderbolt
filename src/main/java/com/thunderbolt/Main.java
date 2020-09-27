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

import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.blockchain.StandardBlockchainCommitter;
import com.thunderbolt.blockchain.contracts.IBlockchainCommitter;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.Node;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.persistence.StandardPersistenceService;
import com.thunderbolt.persistence.contracts.IContiguousStorage;
import com.thunderbolt.persistence.contracts.IMetadataProvider;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.transaction.MemoryTransactionsPoolService;
import com.thunderbolt.transaction.StandardTransactionValidator;
import com.thunderbolt.transaction.contracts.ITransactionValidator;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    // Constants
    static private final String USER_HOME_PATH   = System.getProperty("user.home");
    static private final String DATA_FOLDER_NAME = ".thunderbolt";
    static private final Path   DEFAULT_PATH     = Paths.get(USER_HOME_PATH, DATA_FOLDER_NAME);
    static private final Path   BLOCKS_PATH      = Paths.get(DEFAULT_PATH.toString(), "blocks");
    static private final Path   REVERT_PATH      = Paths.get(DEFAULT_PATH.toString(), "reverts");
    static private final Path   METADATA_PATH    = Paths.get(DEFAULT_PATH.toString(), "metadata");
    static private final Path   WALLET_PATH      = Paths.get(USER_HOME_PATH.toString(), "wallet.bin");
    static private final Path   WALLET_PATH_1    = Paths.get(USER_HOME_PATH.toString(), "wallet1.bin");
    static private final Path   WALLET_PATH_2    = Paths.get(USER_HOME_PATH.toString(), "wallet2.bin");

    static private final String BLOCK_PATTERN    = "block%05d.bin";
    static private final String REVERT_PATTERN   = "revert%05d.bin";

    private static final Logger s_logger = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException, ProtocolException, InterruptedException, StorageException
    {
        IPersistenceService      persistenceService    = createPersistenceService();
        ITransactionsPoolService memPool               = new MemoryTransactionsPoolService();
        ITransactionValidator    transactionValidator  = new StandardTransactionValidator(persistenceService, NetworkParameters.mainNet());
        IBlockchainCommitter     committer             = new StandardBlockchainCommitter(persistenceService, memPool);
        Blockchain               blockchain            = new Blockchain(NetworkParameters.mainNet(), transactionValidator, committer, persistenceService);

        Node node = new Node(NetworkParameters.mainNet(), blockchain, memPool);
        node.start();

        while (true)
        {
            Thread.sleep(2000);
            node.pingAll();
        }
        /*
        StandardPeerDiscoverer PeerDiscoverer = new StandardPeerDiscoverer();
        InetSocketAddress[] peers = PeerDiscoverer.getPeers();

        ServerSocket serverSocket = new ServerSocket(NetworkParameters.mainNet().getPort());
        serverSocket.setSoTimeout(0);

        System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
        Socket server = serverSocket.accept();

        System.out.println("Just connected to " + server.getRemoteSocketAddress());

        while(true)
        {
            Thread.sleep(500);
            while (server.getInputStream().available() > 0)
            {/*
                System.out.println(server.getInputStream().available());
                byte[] message = server.getInputStream().readNBytes(server.getInputStream().available());
                s_logger.debug(Convert.toHexString(message));

                ProtocolMessage protocolMessage = new ProtocolMessage(server.getInputStream(), NetworkParameters.mainNet().getPacketMagic());
                PingPayload payload = new PingPayload(ByteBuffer.wrap(protocolMessage.getPayload()));
                s_logger.debug("{}", payload.getNonce());
            }
        }*/

        /*
        if (peers[1].getAddress().isReachable(1000))
        {
            Connection connection = new Connection(NetworkParameters.mainNet(), peers[1], 0, 1000);
            connection.ping();
            ProtocolMessage message = connection.receive();
        }*/

        /*
        IPersistenceService      persistenceService    = createPersistenceService();
        ITransactionsPoolService memPool               = new MemoryTransactionsPoolService();
        ITransactionValidator    transactionValidator  = new StandardTransactionValidator(persistenceService, NetworkParameters.mainNet());
        IBlockchainCommitter     committer             = new StandardBlockchainCommitter(persistenceService, memPool);
        Blockchain               blockchain            = new Blockchain(NetworkParameters.mainNet(), transactionValidator, committer, persistenceService);

        Wallet wallet = new Wallet(WALLET_PATH.toString(), "1234");
        wallet.initialize(persistenceService);
        s_logger.debug(wallet.getBalance().toString());
        s_logger.debug(wallet.getAddress().toString());

        Wallet wallet1 = new Wallet(WALLET_PATH_1.toString(), "1234");
        wallet1.initialize(persistenceService);
        s_logger.debug(wallet1.getBalance().toString());
        s_logger.debug(wallet1.getAddress().toString());

        Wallet wallet2 = new Wallet(WALLET_PATH_2.toString(), "1234");
        wallet2.initialize(persistenceService);
        s_logger.debug(wallet2.getBalance().toString());
        s_logger.debug(wallet2.getAddress().toString());

        //Transaction newTransaction = wallet.createTransaction(BigInteger.valueOf(0), wallet1.getAddress());
        //memPool.addTransaction(newTransaction);

        blockchain.addOutputsUpdateListener(wallet);
        blockchain.addOutputsUpdateListener(wallet1);
        blockchain.addOutputsUpdateListener(wallet2);

        StandardMiner miner = new StandardMiner(memPool, blockchain, wallet2);
        Block parent = NetworkParameters.createGenesis();

        for (int  i = 0; i < 15; ++i)
        {
            Block newBlock = miner.mine(parent, i);
            blockchain.add(newBlock);
            parent = newBlock;
            s_logger.debug(wallet.getBalance().toString());
            s_logger.debug(wallet1.getBalance().toString());
            s_logger.debug(wallet2.getBalance().toString());
        }*/
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
}