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
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * The persistence manager is a naive approach at storing and retrieving data (and metadata) for the network state (block
 * and transaction artifacts). We will need to improve upon this in the future.
 *
 * All the items are indexed by the ID in the system (the hash of the serialized data).
 *
 * At initialization time this class will load in memory the list of available blocks and unspent transaction outputs.
 *
 * TODO: We need to improve this class to make it unit testable. Right now is just a bunch of naive methods for storing
 * and retrieving blocks.
 */
public class PersistenceManager
{
    private static final Logger s_logger = LoggerFactory.getLogger(PersistenceManager.class);

    // Constants
    static public final String USER_HOME_PATH       = System.getProperty("user.home");
    static public final String DATA_FOLDER_NAME     = ".thunderbolt";
    static public final Path   DEFAULT_PATH         = Paths.get(USER_HOME_PATH, DATA_FOLDER_NAME);
    static public final Path   BLOCKS_PATH          = Paths.get(DEFAULT_PATH.toString(), "blocks");
    static public final Path   BLOCKS_METADATA_PATH = Paths.get(BLOCKS_PATH.toString(), "manifest");
    static public final Path   STATE_PATH           = Paths.get(DEFAULT_PATH.toString(), "state");
    static public final long   BLOCKS_FILE_SIZE     = 1024 * 1000 * 128 ; // 128 MB
    static private final int   BLOCKS_FILE_MAGIC    = 0xAAAAAAAA;

    // Singleton Instance
    private static final PersistenceManager instance = new PersistenceManager();

    /**
     * Defeats instantiation of the PersistenceManager class.
     */
    private PersistenceManager()
    {
        s_logger.debug("Initializing persistence manager...");

        s_logger.debug(BLOCKS_PATH.toString());
        // Initialize.
        if (!BLOCKS_PATH.toFile().exists())
        {
            s_logger.debug(String.format("Block data folder '%s' does not exist. Creating...", BLOCKS_PATH.toString()));
            BLOCKS_PATH.toFile().mkdirs();

            s_logger.debug("Initializing block manifest...");
            BlocksManifest.initialize();
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
     *
     * @param height The height of this block.
     */
    public void persist(Block block, int height) throws IOException
    {
        int    lastBlock       = BlocksManifest.getLastUsedFile();
        long   position        = 0;
        byte[] serializedBlock = block.serialize();

        // The list of unspent transaction outputs this blocks spends.
        List<UnspentTransactionOutput> unspentTransactionOutputs = new ArrayList<>();

        for (int i = 0; i < block.getTransactionsCount(); ++i)
        {
            Transaction transaction = block.getTransaction(i);

            // We ignore coinbase transactions since they dont spent any previous outputs.
            if (transaction.isCoinBase())
                continue;

            for (TransactionInput input: transaction.getInputs())
            {
                Hash transactionHash = input.getReferenceHash();
                int  outputIndex     = input.getIndex();

                Transaction referencedTransaction = getTransaction(transactionHash);

                UnspentTransactionOutput unspentOutput = new UnspentTransactionOutput();
                unspentOutput.setBlockHeight(height);
                unspentOutput.setVersion(referencedTransaction.getVersion());
                unspentOutput.setIsCoinbase(referencedTransaction.isCoinBase());
                unspentOutput.getOutputs().add(referencedTransaction.getOutputs().get(outputIndex));

                unspentTransactionOutputs.add(unspentOutput);
            }
        }

        File file = Paths.get(PersistenceManager.BLOCKS_PATH.toString(), String.format("block%05d.bin", lastBlock)).toFile();

        if (file.exists())
        {
            long size = file.length();

            // If the is file bigger than we expect; create a new file.
            if (size > BLOCKS_FILE_SIZE)
            {
                s_logger.debug(String.format("File %s is already full, creating new file...", lastBlock));

                lastBlock = BlocksManifest.getNextBlocksFileName();

                file = Paths.get(PersistenceManager.BLOCKS_PATH.toString(), String.format("block%05d.bin", lastBlock)).toFile();

                FileOutputStream fileStream = new FileOutputStream(new File(file.toString()), true);

                fileStream.write(NumberSerializer.serialize(BLOCKS_FILE_MAGIC));
                fileStream.write(NumberSerializer.serialize(serializedBlock.length));
                fileStream.write(serializedBlock);

                // Serialize the revert data.
                ByteArrayOutputStream data = new ByteArrayOutputStream();

                data.write(unspentTransactionOutputs.size());
                for (UnspentTransactionOutput unspent: unspentTransactionOutputs)
                    data.write(unspent.serialize());

                fileStream.write(NumberSerializer.serialize(data.size()));
                fileStream.write(data.toByteArray());

                fileStream.close();
            }
            else
            {
                position = size;
                FileOutputStream fileStream = new FileOutputStream(new File(file.toString()), true);

                fileStream.write(NumberSerializer.serialize(BLOCKS_FILE_MAGIC));
                fileStream.write(NumberSerializer.serialize(serializedBlock.length));
                fileStream.write(serializedBlock);

                // Serialize the revert data.
                ByteArrayOutputStream data = new ByteArrayOutputStream();

                data.write(unspentTransactionOutputs.size());
                for (UnspentTransactionOutput unspent: unspentTransactionOutputs)
                    data.write(unspent.serialize());

                fileStream.write(NumberSerializer.serialize(data.size()));
                fileStream.write(data.toByteArray());

                fileStream.close();
            }
        }
        else
        {
            FileOutputStream fileStream = new FileOutputStream(new File(file.toString()), true);

            fileStream.write(NumberSerializer.serialize(BLOCKS_FILE_MAGIC));
            fileStream.write(NumberSerializer.serialize(serializedBlock.length));
            fileStream.write(serializedBlock);

            // Serialize the revert data.
            ByteArrayOutputStream data = new ByteArrayOutputStream();

            data.write(unspentTransactionOutputs.size());
            for (UnspentTransactionOutput unspent: unspentTransactionOutputs)
                data.write(unspent.serialize());

            fileStream.write(NumberSerializer.serialize(data.size()));
            fileStream.write(data.toByteArray());

            fileStream.close();
        }

        s_logger.debug(String.format("File saved to '%s', position '%s'", String.format("block%05d.bin", lastBlock), position));

        BlockMetadata metadata = new BlockMetadata();

        metadata.setHeader(block.getHeader());
        metadata.setBlockFile(lastBlock);
        metadata.setBlockFilePosition(position);
        metadata.setSpentOutputsPosition(position + serializedBlock.length);
        metadata.setTransactionCount(block.getTransactionsCount());
        metadata.setHeight(height);
        metadata.setStatus((byte)0);

        BlocksManifest.addBlockMetadata(metadata);

        // Create and store the transaction metadata for this block.
        for (int i = 0; i < block.getTransactionsCount(); ++i)
        {
            Transaction transaction = block.getTransaction(i);

            TransactionMetadata transactionMetadata = new TransactionMetadata();
            transactionMetadata.setBlockFile(lastBlock);
            transactionMetadata.setBlockPosition(position);
            transactionMetadata.setTransactionPosition(i);
            transactionMetadata.setHash(transaction.getTransactionId());

            BlocksManifest.addTransactionMetadata(transactionMetadata);
        }
    }

    /**
     * Gets the Block with the given hash.
     */
    public Block getBlock(Hash hash) throws IOException
    {
        BlockMetadata metadata = BlocksManifest.getBlockMetadata(hash);

        Path filePath = Paths.get(
                PersistenceManager.BLOCKS_PATH.toString(),
                String.format("block%05d.bin", metadata.getBlockFile()));

        InputStream data = Files.newInputStream(filePath);
        data.skip(metadata.getBlockFilePosition());

        byte[] entryHeader = new byte[Integer.BYTES * 2];
        data.read(entryHeader, 0, Integer.BYTES * 2);

        ByteBuffer buffer = ByteBuffer.wrap(entryHeader);
        int magic = buffer.getInt();
        int size  = buffer.getInt();

        if (magic != BLOCKS_FILE_MAGIC)
            throw new IOException("Invalid magic header.");

        byte[] rawBlock = new byte[size];
        data.read(rawBlock, 0, size);

        Block block = new Block(ByteBuffer.wrap(rawBlock));

        return block;
    }

    /**
     * Gets the spent outputs for the block with the given hash.
     *
     * @param hash The block hash.
     *
     * @return the spent outputs by this block.
     */
    public List<UnspentTransactionOutput> getSpentOutputs(Hash hash) throws IOException
    {
        BlockMetadata metadata = BlocksManifest.getBlockMetadata(hash);

        Path filePath = Paths.get(
                PersistenceManager.BLOCKS_PATH.toString(),
                String.format("block%05d.bin", metadata.getBlockFile()));

        InputStream data = Files.newInputStream(filePath);
        data.skip(metadata.getSpentOutputsPosition());

        byte[] dataSize = new byte[Integer.BYTES];
        data.read(dataSize, 0, Integer.BYTES);

        ByteBuffer buffer = ByteBuffer.wrap(dataSize);
        int count  = buffer.getInt();

        ArrayList<UnspentTransactionOutput> outputs = new ArrayList<>();

        for (int i = 0; i < count; ++i)
            outputs.add(new UnspentTransactionOutput(buffer));

        return outputs;
    }

    /**
     * Gets the current chain head.
     *
     * @return The block at the head of the blockchain.
     */
    public BlockMetadata getChainHead() throws IOException
    {
        return BlocksManifest.getChainHead();
    }

    /**
     * Sets the current chain head.
     *
     * @param metadata The block at the head of the blockchain.
     */
    public void setChainHead(BlockMetadata metadata) throws IOException
    {
        BlocksManifest.setChainHead(metadata);
    }

    /**
     * Gets the transaction with the given hash.
     *
     * @param hash The transaction id.
     *
     * @return The transaction.
     */
    public Transaction getTransaction(Hash hash) throws IOException
    {
        TransactionMetadata metadata = BlocksManifest.getTransactionMetadata(hash);

        Path filePath = Paths.get(
                PersistenceManager.BLOCKS_PATH.toString(),
                String.format("block%05d.bin", metadata.getBlockFile()));

        InputStream data = Files.newInputStream(filePath);
        data.skip(metadata.getBlockPosition());

        byte[] entryHeader = new byte[Integer.BYTES * 2];
        data.read(entryHeader, 0, Integer.BYTES * 2);

        ByteBuffer buffer = ByteBuffer.wrap(entryHeader);
        int magic = buffer.getInt();
        int size  = buffer.getInt();

        if (magic != BLOCKS_FILE_MAGIC)
            throw new IOException("Invalid magic header.");

        byte[] rawBlock = new byte[size];
        data.read(rawBlock, 0, size);

        Block block = new Block(ByteBuffer.wrap(rawBlock));

        return block.getTransaction(metadata.getTransactionPosition());
    }

    /**
     * Gets the unspent output that matches the given transaction id and index inside that transaction.
     *
     * @param transactionId The transaction ID that contains the output.
     *
     * @return The transaction output, or null if the output is not available or was already spent.
     */
    public UnspentTransactionOutput getUnspentOutput(Hash transactionId) throws IOException
    {
        return BlocksManifest.getUnspentOutput(transactionId);
    }

    /**
     * Adds the given unspent output to the database.
     *
     * @param output The unspent output to store in the system.
     */
    public void addUnspentOutput(UnspentTransactionOutput output) throws IOException
    {
        s_logger.debug(String.format("Adding outputs for transaction %s", output.getHash().toString()));
        BlocksManifest.addUnspentOutput(output);
    }
}
