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
import com.thunderbolt.persistence.contracts.IChainHeadUpdateListener;
import com.thunderbolt.persistence.contracts.IContiguousStorage;
import com.thunderbolt.persistence.contracts.IMetadataProvider;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.*;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Ripemd160Digester;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.transaction.parameters.SingleSignatureParameters;
import com.thunderbolt.wallet.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
    private IContiguousStorage m_blockStorage     = null;
    private IContiguousStorage m_revertsStorage   = null;
    private IMetadataProvider  m_metadataProvider = null;

    // Event listeners.
    private final List<IChainHeadUpdateListener> m_listeners = new ArrayList<>();

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
                transactionMetadata.setBlockHash(block.getHeaderHash());
                transactionMetadata.setBlockHeight(height);
                transactionMetadata.setTimestamp(block.getHeader().getTimeStamp());

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
    public Block getBlock(Sha256Hash sha256Hash) throws StorageException
    {
        BlockMetadata metadata = m_metadataProvider.getBlockMetadata(sha256Hash);

        StoragePointer pointer = new StoragePointer();
        pointer.segment = metadata.getBlockSegment();
        pointer.offset = metadata.getBlockOffset();

        byte[] rawBlock = m_blockStorage.retrieve(pointer);

        return new Block(ByteBuffer.wrap(rawBlock));
    }

    /**
     * Gets the Block metadata with the given hash.
     *
     * @param sha256Hash The hash of the block.
     *
     * @return The block metadata.
     */
    public BlockMetadata getBlockMetadata(Sha256Hash sha256Hash)
    {
        return m_metadataProvider.getBlockMetadata(sha256Hash);
    }

    /**
     * Gets whether we already persisted a block with this header.
     *
     * @param sha256Hash The hash of the header of the block.
     *
     * @return true if we have the block; otherwise; false.
     */
    public boolean hasBlockMetadata(Sha256Hash sha256Hash)
    {
        return m_metadataProvider.hasBlockMetadata(sha256Hash);
    }

    /**
     * Gets the spent outputs for the block with the given hash.
     *
     * @param sha256Hash The block hash.
     *
     * @return the spent outputs by this block.
     */
    public List<UnspentTransactionOutput> getSpentOutputs(Sha256Hash sha256Hash) throws StorageException
    {
        BlockMetadata metadata = m_metadataProvider.getBlockMetadata(sha256Hash);

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
    public BlockMetadata getChainHead()
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

        // Notify the listeners.
        for (IChainHeadUpdateListener listener : m_listeners)
            listener.onChainHeadChanged(metadata.getHeader());
    }

    /**
     * Gets the transaction with the given hash.
     *
     * @param sha256Hash The transaction id.
     *
     * @return The transaction.
     */
    public Transaction getTransaction(Sha256Hash sha256Hash) throws StorageException
    {
        TransactionMetadata metadata = m_metadataProvider.getTransactionMetadata(sha256Hash);

        StoragePointer pointer = new StoragePointer();
        pointer.segment = metadata.getBlockFile();
        pointer.offset = metadata.getBlockPosition();

        byte[] rawBlock = m_blockStorage.retrieve(pointer);

        Block block = new Block(ByteBuffer.wrap(rawBlock));

        return block.getTransaction(metadata.getTransactionPosition());
    }

    /**
     * Gets the metadata transaction with the given hash.
     *
     * @param sha256Hash The transaction id.
     *
     * @return The transaction metadata.
     */
    public TransactionMetadata getTransactionMetadata(Sha256Hash sha256Hash) throws StorageException
    {
        return m_metadataProvider.getTransactionMetadata(sha256Hash);
    }

    /**
     * Gets all the transactions incoming or outgoing from this address.
     *
     * @param address The address of the wallet to get the transactions for.
     *
     * @return An array with all the addresses related to a given public address.
     */
    public List<Transaction> getTransactionsForAddress(Address address) throws StorageException
    {
        List<Transaction> result = new ArrayList<>();

        BlockMetadata cursor = getChainHead();

        while (!cursor.getHeader().getParentBlockHash().equals(new Sha256Hash()))
        {
            Block block = getBlock(cursor.getHash());
            List<Transaction> transactions = block.getTransactions();

            for (Transaction transaction: transactions)
            {
                boolean detected = false;
                for (TransactionOutput output : transaction.getOutputs())
                {
                    if (Arrays.equals(output.getLockingParameters(), address.getPublicHash()))
                    {
                        result.add(transaction);
                        detected = true;
                        break;
                    }
                }

                // We already know this transaction mention us, so we do not need to keep looking forward.
                if (detected)
                    continue;

                for (TransactionInput input : transaction.getInputs())
                {
                    if (input.isCoinBase())
                        continue;

                    SingleSignatureParameters params = new SingleSignatureParameters(input.getUnlockingParameters());

                    if (Arrays.equals(params.getPublicKeyHash(), address.getPublicHash()))
                    {
                        result.add(transaction);
                        break;
                    }
                }
            }

            cursor = getBlockMetadata(cursor.getHeader().getParentBlockHash());
        }

        return result;
    }

    /**
     * Gets the unspent output that matches the given transaction id and index inside that transaction.
     *
     * @param transactionId The transaction ID that contains the output.
     *
     * @return The transaction output, or null if the output is not available or was already spent.
     */
    public UnspentTransactionOutput getUnspentOutput(Sha256Hash transactionId, int index)
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
     * Gets all the unspent outputs of a given public key.
     *
     * @param address The address of the wallet to get the unspent outputs for.
     *
     * @return An array with all the unspent outputs related to a given public address.
     */
    public List<UnspentTransactionOutput> getUnspentOutputsForAddress(Address address) throws StorageException
    {
        return m_metadataProvider.getUnspentOutputsForAddress(address);
    }

    /**
     * Gets all the unspent outputs.
     *
     * @return An array with all the unspent outputs.
     */
    @Override
    public List<UnspentTransactionOutput> getUnspentOutputs()
    {
        return m_metadataProvider.getUnspentOutputs();
    }

    /**
     * Removes the unspent output transaction from the metadata provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    public boolean removeUnspentOutput(Sha256Hash id, int index) throws StorageException
    {
        return m_metadataProvider.removeUnspentOutput(id, index);
    }

    /**
     * Gets whether we have persisted this transaction or not.
     *
     * @param sha256Hash The id of the transaction.
     *
     * @return true if the transaction is already present; otherwise false;
     */
    public boolean hasTransaction(Sha256Hash sha256Hash)
    {
        return m_metadataProvider.hasTransaction(sha256Hash);
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
                    Sha256Hash transactionSha256Hash = input.getReferenceHash();
                    int  outputIndex     = input.getIndex();

                    Transaction referencedTransaction = getTransaction(transactionSha256Hash);

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

    /**
     * Adds a new listener to the list of chain head update listeners. This listener will be notified when a change
     * regarding the chain head occurs.
     *
     * @param listener The new listener to be added.
     */
    @Override
    public void addChainHeadUpdateListener(IChainHeadUpdateListener listener)
    {
        m_listeners.add(listener);
    }
}
