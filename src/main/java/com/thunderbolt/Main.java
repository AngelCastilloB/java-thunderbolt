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
import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.ServiceLocator;
import com.thunderbolt.mining.MiningException;
import com.thunderbolt.mining.StandardMiner;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.StandardPersistenceService;
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.security.*;
import com.thunderbolt.transaction.*;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import com.thunderbolt.wallet.Address;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
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
    public static void main(String[] args) throws IOException, GeneralSecurityException, StorageException, MiningException
    {
        MemoryTransactionsPoolService memPool = new MemoryTransactionsPoolService();

        initializeServices();

        Wallet wallet = new Wallet(WALLET_PATH.toString(), "1234");
        wallet.initialize();
        s_logger.debug(wallet.getBalance().toString());
        s_logger.debug(wallet.getAddress().toString());

        Wallet wallet1 = new Wallet(WALLET_PATH_1.toString(), "1234");
        wallet1.initialize();
        s_logger.debug(wallet1.getBalance().toString());
        s_logger.debug(wallet1.getAddress().toString());

        Wallet wallet2 = new Wallet(WALLET_PATH_2.toString(), "1234");
        wallet2.initialize();
        s_logger.debug(wallet2.getBalance().toString());
        s_logger.debug(wallet2.getAddress().toString());

        Transaction newTransaction = wallet1.createTransaction(BigInteger.valueOf(23L), wallet.getAddress());
        memPool.addTransaction(newTransaction);

        Blockchain blockchain = new Blockchain(NetworkParameters.mainNet());
        blockchain.addOutputsUpdateListener(wallet);
        blockchain.addOutputsUpdateListener(wallet1);
        blockchain.addOutputsUpdateListener(wallet2);

        StandardMiner miner = new StandardMiner(memPool, blockchain, wallet2);
        Block newBlock = miner.mine();
        blockchain.add(newBlock);

        s_logger.debug(wallet.getBalance().toString());
        s_logger.debug(wallet1.getBalance().toString());
        s_logger.debug(wallet2.getBalance().toString());
    }

    /**
     * Initializes the persistence manager.
     *
     * @throws StorageException If there is any error opening the storage.
     */
    static void initializeServices() throws StorageException
    {
        DiskContiguousStorage   blockStorage     = new DiskContiguousStorage(BLOCKS_PATH, BLOCK_PATTERN);
        DiskContiguousStorage   revertsStorage   = new DiskContiguousStorage(REVERT_PATH, REVERT_PATTERN);
        LevelDbMetadataProvider metadataProvider = new LevelDbMetadataProvider(METADATA_PATH);

        ServiceLocator.register(
                IPersistenceService.class,
                new StandardPersistenceService(blockStorage, revertsStorage, metadataProvider));

        ServiceLocator.register(
                ITransactionsPoolService.class,
                new MemoryTransactionsPoolService());
    }
}
