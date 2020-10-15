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

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.blockchain.StandardBlockchainCommitter;
import com.thunderbolt.blockchain.contracts.IBlockchainCommitter;
import com.thunderbolt.common.Convert;
import com.thunderbolt.mining.MiningException;
import com.thunderbolt.mining.StandardMiner;
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
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.transaction.MemoryTransactionsPoolService;
import com.thunderbolt.transaction.StandardTransactionValidator;
import com.thunderbolt.transaction.contracts.ITransactionValidator;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

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
    static private final Path   ADDRESS_PATH     = Paths.get(DEFAULT_PATH.toString(), "peers");

    static private final Path   WALLET_PATH      = Paths.get(USER_HOME_PATH, "wallet.bin");
    static private final Path   WALLET_PATH_1    = Paths.get(USER_HOME_PATH, "wallet1.bin");
    static private final Path   WALLET_PATH_2    = Paths.get(USER_HOME_PATH, "wallet2.bin");

    static private final String BLOCK_PATTERN    = "block%05d.bin";
    static private final String REVERT_PATTERN   = "revert%05d.bin";

    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(Main.class);
    private static Thread       s_miningThread = null;

    // Mining test.
    private static Block s_miningChain = null; //TODO: remove
    private static int s_height = 0;
    private static StandardMiner s_miner = null;

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException, StorageException, GeneralSecurityException
    {
        IPersistenceService           persistenceService     = createPersistenceService();
        MemoryTransactionsPoolService memPool                = new MemoryTransactionsPoolService(persistenceService);
        ITransactionValidator         transactionValidator   = new StandardTransactionValidator(persistenceService, NetworkParameters.mainNet());
        IBlockchainCommitter          committer              = new StandardBlockchainCommitter(persistenceService, memPool);
        Blockchain                    blockchain             = new Blockchain(NetworkParameters.mainNet(), transactionValidator, committer, persistenceService);
        IPeerDiscoverer               discoverer             = new StandardPeerDiscoverer();
        INetworkAddressPool           addressPool            = new LevelDbNetworkAddressPool(ADDRESS_PATH);

        blockchain.addOutputsUpdateListener(memPool);

        s_logger.debug(memPool.toString());
        Wallet wallet = new Wallet(WALLET_PATH.toString(), "1234");
        wallet.initialize(persistenceService);
        s_logger.debug(wallet.getBalance().toString());
        s_logger.debug(wallet.getAddress().toString());

        s_miner = new StandardMiner(memPool, blockchain, wallet);

        ProtocolMessageFactory.initialize(NetworkParameters.mainNet(), persistenceService);

        PeerManager peerManager = new PeerManager(
                1,
                4,
                3600000,// 1 hour
                1200000, // 20 minutes
                discoverer,
                NetworkParameters.mainNet(),
                addressPool);

        if (!peerManager.start())
        {
            s_logger.debug("The peer manager could not be started. The node will shutdown");
            return;
        }

        Node node = new Node(NetworkParameters.mainNet(), blockchain, memPool, peerManager, persistenceService);

        // TODO: Remove this, only for testing purposes.
        s_miningThread = new Thread(() -> { startMining(blockchain, 1000); });
        s_miningChain = persistenceService.getBlock(persistenceService.getChainHead().getHash());
        s_height = (int)blockchain.getChainHead().getHeight();

        if (true)
            s_miningThread.start();

        node.run();
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
     * Start mining new blocks.
     *
     * @param blockchain The current blockchain.
     * @param delayBetweenBlocks The delay between new blocks.
     */
    private static void startMining(Blockchain blockchain, int delayBetweenBlocks)
    {
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        while (true)
        {
            try
            {
                Block newBlock = s_miner.mine(s_miningChain.getHeaderHash(), s_height);
                ++s_height;

                s_miningChain = newBlock;
                blockchain.add(newBlock);

                Thread.sleep(delayBetweenBlocks);
            }
            catch (MiningException | StorageException e)
            {
                s_logger.error("An error has occur while mining the new block: ", e);
            }
            catch (InterruptedException e)
            {
                // The thread was interrupted, exiting...
                s_logger.error("Thread interrupted");
                return;
            }
        }
    }
}