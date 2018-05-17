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
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * The persistence service is a naive approach at storing and retrieving data (and metadata) for the network state (block
 * and transaction artifacts). We will need to improve upon this in the future.
 *
 * All the items are indexed by the ID in the system (the hash of the serialized data).
 */
public class StandardPersistenceService implements IPersistenceService
{
    private static final Logger s_logger = LoggerFactory.getLogger(StandardPersistenceService.class);

    // Instance fields
    private IContiguousStorage     m_blockStorage     = null;
    private IContiguousStorage     m_revertsStorage   = null;
    private IMetadataProvider      m_metadataProvider = null;

    /**
     * Initializes an instance of the persistence service.
     *
     * @param blockStorage     The storage for the blocks.
     * @param revertStorage    The storage for the revert data.
     * @param metadataProvider The blockchain metadata provider.
     */
    public StandardPersistenceService(IContiguousStorage blockStorage, IContiguousStorage revertStorage, IMetadataProvider metadataProvider)
    {
        m_blockStorage     = blockStorage;
        m_revertsStorage   = revertStorage;
        m_metadataProvider = metadataProvider;
    }

    /**
     * Persist the given block. The block will be indexed by its block id (hash).
     *
     * @param block     The block to persist.
     * @param height    The height of this block.
     * @param totalWork The total amount of work on the chain up to this point.
     *
     * @return The newly created BlockMetadata.
     */
    public BlockMetadata persist(Block block, long height, BigInteger totalWork) throws StorageException
    {
        BlockMetadata metadata = new BlockMetadata();

        try
        {
            byte[] serializedBlock = block.serialize();
            byte[] revertData      = getRevertData(block, height);

            StoragePointer blockPointer = m_blockStorage.store(serializedBlock);
            StoragePointer revertPointer = m_revertsStorage.store(revertData);

            metadata.setHeader(block.getHeader());
            metadata.setBlockSegment(blockPointer.segment);
            metadata.setBlockOffset(blockPointer.offset);
            metadata.setRevertSegment(revertPointer.segment);
            metadata.setRevertOffset(revertPointer.offset);
            metadata.setTransactionCount(block.getTransactionsCount());
            metadata.setHeight(height);
            metadata.setStatus((byte)0);
            metadata.setTotalWork(totalWork);

            m_metadataProvider.addBlockMetadata(metadata);

            // Create and store the transaction metadata for this block.
            for (int i = 0; i < block.getTransactionsCount(); ++i)
            {
                Transaction transaction = block.getTransaction(i);

                TransactionMetadata transactionMetadata = new TransactionMetadata();
                transactionMetadata.setBlockFile(blockPointer.segment);
                transactionMetadata.setBlockPosition(blockPointer.offset);
                transactionMetadata.setTransactionPosition(i);
                transactionMetadata.setHash(transaction.getTransactionId());

                m_metadataProvider.addTransactionMetadata(transactionMetadata);
            }
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to persist block '%s'", block.getHeaderHash()), exception);
        }

        return metadata;
    }

    /**
     * Gets the Block with the given hash.
     */
    public Block getBlock(Hash hash) throws StorageException
    {
        BlockMetadata metadata = m_metadataProvider.getBlockMetadata(hash);

        StoragePointer pointer = new StoragePointer();
        pointer.segment = metadata.getBlockSegment();
        pointer.offset = metadata.getBlockOffset();

        byte[] rawBlock = m_blockStorage.retrieve(pointer);

        return new Block(ByteBuffer.wrap(rawBlock));
    }

    /**
     * Gets the Block metadata with the given hash.
     *
     * @param hash The hash of the block.
     *
     * @return The block metadata.
     */
    public BlockMetadata getBlockMetadata(Hash hash) throws StorageException
    {
        return m_metadataProvider.getBlockMetadata(hash);
    }

    /**
     * Gets the spent outputs for the block with the given hash.
     *
     * @param hash The block hash.
     *
     * @return the spent outputs by this block.
     */
    public List<UnspentTransactionOutput> getSpentOutputs(Hash hash) throws StorageException
    {
        BlockMetadata metadata = m_metadataProvider.getBlockMetadata(hash);

        StoragePointer pointer = new StoragePointer();
        pointer.segment = metadata.getRevertSegment();
        pointer.offset  = metadata.getRevertOffset();

        byte[] revertData = m_revertsStorage.retrieve(pointer);

        ByteBuffer buffer = ByteBuffer.wrap(revertData);

        int transactionCount = buffer.getInt();

        ArrayList<UnspentTransactionOutput> outputs = new ArrayList<>();

        for (int i = 0; i < transactionCount; ++i)
            outputs.add(new UnspentTransactionOutput(buffer));

        return outputs;
    }

    /**
     * Gets the current chain head.
     *
     * @return The block at the head of the blockchain.
     */
    public BlockMetadata getChainHead() throws StorageException
    {
        return m_metadataProvider.getChainHead();
    }

    /**
     * Sets the current chain head.
     *
     * @param metadata The block at the head of the blockchain.
     */
    public void setChainHead(BlockMetadata metadata) throws StorageException
    {
        m_metadataProvider.setChainHead(metadata);
    }

    /**
     * Gets the transaction with the given hash.
     *
     * @param hash The transaction id.
     *
     * @return The transaction.
     */
    public Transaction getTransaction(Hash hash) throws StorageException
    {
        TransactionMetadata metadata = m_metadataProvider.getTransactionMetadata(hash);

        StoragePointer pointer = new StoragePointer();
        pointer.segment = metadata.getBlockFile();
        pointer.offset = metadata.getBlockPosition();

        byte[] rawBlock = m_blockStorage.retrieve(pointer);

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
    public UnspentTransactionOutput getUnspentOutput(Hash transactionId, int index) throws StorageException
    {
        return m_metadataProvider.getUnspentOutput(transactionId, index);
    }

    /**
     * Adds the given unspent output to the database.
     *
     * @param output The unspent output to store in the system.
     */
    public boolean addUnspentOutput(UnspentTransactionOutput output) throws StorageException
    {
        return m_metadataProvider.addUnspentOutput(output);
    }

    /**
     * Removes the unspent output transaction from the metadata provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    public boolean removeUnspentOutput(Hash id, int index) throws StorageException
    {
        return m_metadataProvider.removeUnspentOutput(id, index);
    }

    /**
     * Gets the revert data for this block.
     *
     * The revert data consist of a list of all the outputs the transactions in this block spends, this transactions
     * need to be added back to the UTXO pool.
     *
     * @param block  The block to get the revert data from.
     * @param height The height of the block.
     *
     * @return Returns the list of spent outputs in a serialized form.
     *
     * @throws StorageException If there is an error querying the required metadata to create the revert data.
     */
    public byte[] getRevertData(Block block, long height) throws StorageException
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            // The list of unspent transaction outputs this blocks spends.
            List<UnspentTransactionOutput> unspentTransactionOutputs = new ArrayList<>();

            for (int i = 0; i < block.getTransactionsCount(); ++i)
            {
                Transaction transaction = block.getTransaction(i);

                // We ignore coinbase transactions since they dont spent any previous outputs.
                if (transaction.isCoinbase())
                    continue;

                for (TransactionInput input: transaction.getInputs())
                {
                    Hash transactionHash = input.getReferenceHash();
                    int  outputIndex     = input.getIndex();

                    Transaction referencedTransaction = getTransaction(transactionHash);

                    UnspentTransactionOutput unspentOutput = new UnspentTransactionOutput();
                    unspentOutput.setBlockHeight(height);
                    unspentOutput.setVersion(referencedTransaction.getVersion());
                    unspentOutput.setIsCoinbase(referencedTransaction.isCoinbase());
                    unspentOutput.setTransactionHash(referencedTransaction.getTransactionId());
                    unspentOutput.setIndex(outputIndex);
                    unspentOutput.setOutput(referencedTransaction.getOutputs().get(outputIndex));

                    unspentTransactionOutputs.add(unspentOutput);
                }
            }

            data.write(NumberSerializer.serialize(unspentTransactionOutputs.size()));

            for (UnspentTransactionOutput unspentTransactionOutput : unspentTransactionOutputs)
                data.write(unspentTransactionOutput.serialize());
        }
        catch (Exception exception)
        {
            throw new StorageException(String.format("Unable to serialize the revert data for block '%s'", block.getHeaderHash()), exception);
        }

        return data.toByteArray();
    }
}
