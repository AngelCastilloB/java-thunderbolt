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
package com.thunderbolt.persistence;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/* IMPLEMENTATION ************************************************************/

/**
 * The persistence manager handles storing and retrieving data and metadata for the network state (block and transaction
 * artifacts).
 *
 * All the items are indexed by the ID in the system (the hash of the serialized data).
 *
 * At initialization time this class will load in memory the list of available blocks and the unspent transaction outputs.
 */
public class PersistenceManager
{
    private static final Logger s_logger = LoggerFactory.getLogger(PersistenceManager.class);

    // Constants
    static private final String USER_HOME_PATH                   = System.getProperty("user.home");
    static private final String THUNDERBOLT_DATA_FOLDER_NAME     = ".thunderbolt";
    static private final Path   THUNDERBOLT_DEFAULT_PATH         = Paths.get(USER_HOME_PATH, THUNDERBOLT_DATA_FOLDER_NAME);
    static private final Path   THUNDERBOLT_BLOCKS_PATH          = Paths.get(THUNDERBOLT_DEFAULT_PATH.toString(), "blocks");
    static private final Path   THUNDERBOLT_BLOCKS_METADATA_PATH = Paths.get(THUNDERBOLT_BLOCKS_PATH.toString(), "index");
    static private final Path   THUNDERBOLT_STATE_PATH           = Paths.get(THUNDERBOLT_DEFAULT_PATH.toString(), "state");

    private static final PersistenceManager instance = new PersistenceManager();

    /**
     * Defeats instantiation of the PersistenceManager class.
     */
    private PersistenceManager()
    {
        s_logger.debug("Initializing persistence manager...");

        // Initialize.
        if (!THUNDERBOLT_BLOCKS_PATH.toFile().exists())
        {
            s_logger.debug(String.format("Block data folder '%s' does not exist. Creating...",THUNDERBOLT_BLOCKS_PATH.toString()));
            THUNDERBOLT_BLOCKS_PATH.toFile().mkdirs();
        }

        if (!THUNDERBOLT_BLOCKS_METADATA_PATH.toFile().exists())
        {
            s_logger.debug(String.format("Block metadata folder '%s' does not exist. Creating...",THUNDERBOLT_BLOCKS_METADATA_PATH.toString()));
            THUNDERBOLT_BLOCKS_METADATA_PATH.toFile().mkdirs();
        }

        if (!THUNDERBOLT_STATE_PATH.toFile().exists())
        {
            s_logger.debug(String.format("State data folder '%s' does not exist. Creating...",THUNDERBOLT_STATE_PATH.toString()));
            THUNDERBOLT_STATE_PATH.toFile().mkdirs();
        }
    }

    /**
     * Gets the singleton instance of the persistence manager.
     *
     * @return The singleton instance of the persistence manager.
     */
    public static PersistenceManager getInstance()
    {
        return instance;
    }

    /**
     * Persist the given block. The block will be indexed by its block id (hash).
     */
    public void persist(Block block) throws IOException
    {
        writeFile(Paths.get(THUNDERBOLT_BLOCKS_PATH.toString(), "bkl0001.dat").toString(), block.serialize());

        s_logger.debug(String.format("File saved to %s", Paths.get(THUNDERBOLT_BLOCKS_PATH.toString(), "bkl0001.dat").toString()));
    }

    /**
     * Gets the Block with the given hash.
     */
    public Block get(Hash hash)
    {
        return new Block();
    }

    /**
     * Gets the current chain head.
     *
     * @return The block at the head of the blockchain.
     */
    public Block getChainHead()
    {
        return new Block();
    }

    /**
     * Gets the transaction with the given hash.
     *
     * @param hash The transaction id.
     *
     * @return The transaction.
     */
    public Transaction getTransaction(Hash hash)
    {
        return new Transaction();
    }

    /**
     * Gets the unspent output that matches the given transaction id and index inside that transaction.
     *
     * @param transactionId The transaction ID that contains the output.
     * @param index The index inside that transaction.
     *
     * @return The transaction output, or null if the output is not available or was already spent.
     */
    public TransactionOutput getUnspentOutput(Hash transactionId, int index)
    {
        return new TransactionOutput();
    }

    /**
     * Reads a file for the disk.
     *
     * @param path The file.
     *
     * @return The data of the file.
     *
     * @throws IOException Thrown if the file is not found.
     */
    private byte[] readFile(String path) throws IOException
    {
        File file       = new File(path);
        FileInputStream fileStream = new FileInputStream(file);
        byte[]          data       = new byte[(int) file.length()];

        fileStream.read(data);
        fileStream.close();

        return data;
    }

    /**
     * Writes a writes to the disk.
     *
     * @param path The file.
     * @param data The data to be saved.
     *
     * @throws IOException Thrown if the file is not found.
     */
    private void writeFile(String path, byte[] data) throws IOException
    {
        File             file       = new File(path);
        FileOutputStream fileStream = new FileOutputStream(file);

        fileStream.write(data);
        fileStream.close();
    }

}
