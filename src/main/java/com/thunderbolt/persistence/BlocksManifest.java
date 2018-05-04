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

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import org.iq80.leveldb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;

import java.io.*;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Data structure that holds how are the blocks stored in disk. (which file and at what index).
 *
 * TODO: We need to make this class an instance so we can unit test the Persistence manager. Right now
 * it is a bunch of naive static methods. We need to improve this in the near future.
 */
public class BlocksManifest
{
    private static final Logger s_logger = LoggerFactory.getLogger(BlocksManifest.class);

    /**
     * Initializes the block manifest by creating the block metadata database.
     */
    public static void initialize()
    {
        updateLastFile(0);
    }

    /**
     * Updates the laset used file in the database.
     *
     * @param number The last used file.
     */
    public static boolean updateLastFile(int number)
    {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;

        try
        {
            db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options);
            db.put(bytes("l"), NumberSerializer.serialize(number));
        }
        catch (IOException e)
        {
            s_logger.debug(e.toString());
            return false;
        }
        finally
        {
            try
            {
                db.close();
            }
            catch (IOException e)
            {
                s_logger.debug(e.toString());
            }
        }

        return true;
    }

    /**
     * Gets the name of the last used block.
     *
     * @return The name of the last used block.
     */
    public static int getLastUsedFile() throws IOException
    {
        int     lastFile = 0;
        Options options  = new Options();

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            lastFile = ByteBuffer.wrap(db.get(bytes("l"))).getInt();
        }

        return lastFile;
    }

    /**
     * Gets the next blocks file name.
     *
     * @return Returns the name of the new blocks file.
     */
    public static int getNextBlocksFileName() throws IOException
    {
        int     lastFile = 0;
        Options options  = new Options();

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            lastFile = ByteBuffer.wrap(db.get(bytes("l"))).getInt();
            ++lastFile;

            db.put(bytes("l"), NumberSerializer.serialize(lastFile));
        }

        return lastFile;
    }

    /**
     * Gets the metadata entry from the manifest.
     *
     * @param blockId The hash of the block header.
     *
     * @return The block metadata.
     *
     * @throws IOException If there is any IO error.
     */
    public static BlockMetadata getBlockMetadata(Hash blockId) throws IOException
    {
        BlockMetadata metadata;

        Options options = new Options();
        options.createIfMissing(true);

        ByteArrayOutputStream key = new ByteArrayOutputStream();
        key.write('b');
        key.write(blockId.serialize());

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            byte[] rawHash = db.get(key.toByteArray());

            metadata = new BlockMetadata(ByteBuffer.wrap(rawHash));
        }

        return metadata;
    }

    /**
     * Adds a block metadata entry to the manifest.
     *
     * @param metadata The metadata to be added.
     *
     * @throws IOException If there is any IO error.
     */
    public static void addBlockMetadata(BlockMetadata metadata) throws IOException
    {
        Options options = new Options();
        options.createIfMissing(true);

        ByteArrayOutputStream key = new ByteArrayOutputStream();
        key.write('b');
        key.write(metadata.getHash().serialize());

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            db.put(key.toByteArray(), metadata.serialize());
        }

        s_logger.debug(String.format("Metadata added for block '%s'", metadata.getHash()));
    }

    /**
     * Sets the block chain head in the manifest..
     *
     * @param metadata The metadata of the block chain head.
     *
     * @throws IOException If there is any IO error.
     */
    public static void setChainHead(BlockMetadata metadata) throws IOException
    {
        Options options = new Options();
        options.createIfMissing(true);

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            db.put(bytes("h"), metadata.serialize());
        }

        s_logger.debug(String.format("Chain head metadata added (block '%s')", metadata.getHash()));
    }

    /**
     * Gets the block chain head metadata entry from the manifest.
     *
     * @return The block metadata.
     *
     * @throws IOException If there is any IO error.
     */
    public static BlockMetadata getChainHead() throws IOException
    {
        BlockMetadata metadata;

        Options options = new Options();
        options.createIfMissing(true);

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            byte[] head = db.get(bytes("h"));

            metadata = new BlockMetadata(ByteBuffer.wrap(head));
        }

        return metadata;
    }

    /**
     * Adds a transaction metadata entry to the manifest.
     *
     * @param metadata The metadata to be added.
     *
     * @throws IOException If there is any IO error.
     */
    public static void addTransactionMetadata(TransactionMetadata metadata) throws IOException
    {
        Options options = new Options();
        options.createIfMissing(true);

        ByteArrayOutputStream key = new ByteArrayOutputStream();
        key.write('t');
        key.write(metadata.getHash().serialize());

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            db.put(key.toByteArray(), metadata.serialize());
        }

        key.close();

        s_logger.debug(String.format("Metadata added for transaction '%s'", metadata.getHash()));
    }

    /**
     * Gets the metadata entry from the manifest.
     *
     * @param transactionId The hash of the transaction.
     *
     * @return The transaction metadata.
     *
     * @throws IOException If there is any IO error.
     */
    public static TransactionMetadata getTransactionMetadata(Hash transactionId) throws IOException
    {
        TransactionMetadata metadata;

        Options options = new Options();
        options.createIfMissing(true);

        ByteArrayOutputStream key = new ByteArrayOutputStream();
        key.write('t');
        key.write(transactionId.serialize());

        try (DB db = factory.open(PersistenceManager.BLOCKS_METADATA_PATH.toFile(), options))
        {
            byte[] rawData = db.get(key.toByteArray());

            metadata = new TransactionMetadata(ByteBuffer.wrap(rawData));
        }

        return metadata;
    }

    /**
     * Gets an unspent transaction from the database.
     *
     * @param id The transactiion id that contains the unspent output.
     *
     * @throws IOException If there is any IO error.
     */
    public static UnspentTransactionOutput getUnspentOutput(Hash id) throws IOException
    {
        UnspentTransactionOutput output;

        Options options = new Options();
        options.createIfMissing(true);

        ByteArrayOutputStream key = new ByteArrayOutputStream();
        key.write('c');
        key.write(id.serialize());

        try (DB db = factory.open(PersistenceManager.STATE_PATH.toFile(), options))
        {
            byte[] data = db.get(key.toByteArray());

            output = new UnspentTransactionOutput(ByteBuffer.wrap(data));
        }

        return output;
    }

    /**
     * Adds an unspent transaction to the database.
     *
     * @param output The unspent outputs to be added.
     *
     * @throws IOException If there is any IO error.
     */
    public static void addUnspentOutput(UnspentTransactionOutput output) throws IOException
    {
        Options options = new Options();
        options.createIfMissing(true);

        ByteArrayOutputStream key = new ByteArrayOutputStream();
        key.write('c');
        key.write(output.getHash().serialize());

        try (DB db = factory.open(PersistenceManager.STATE_PATH.toFile(), options))
        {
            db.put(key.toByteArray(), output.serialize());
        }

        s_logger.debug(String.format("Outputs metadata added for transaction '%s'", output.getHash()));
    }
}
