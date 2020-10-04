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
package com.thunderbolt.persistence.contracts;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.wallet.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The persistence service provides the means to store and retrieve data and metadata from the blockchain.
 */
public interface IPersistenceService
{
    /**
     * Persist the given block. The block will be indexed by its block id (hash).
     *
     * @param block     The block to persist.
     * @param height    The height of this block.
     * @param totalWork The total amount of work on the chain up to this point.
     *
     * @return The newly created BlockMetadata.
     */
    BlockMetadata persist(Block block, long height, BigInteger totalWork) throws StorageException;

    /**
     * Gets the Block with the given hash.
     *
     * @param sha256Hash The block hash.
     *
     * @return The block.
     */
    Block getBlock(Sha256Hash sha256Hash) throws StorageException;

    /**
     * Gets the Block metadata with the given hash.
     *
     * @param sha256Hash The hash of the block.
     *
     * @return The block metadata.
     */
    BlockMetadata getBlockMetadata(Sha256Hash sha256Hash);

    /**
     * Gets the spent outputs for the block with the given hash.
     *
     * @param sha256Hash The block hash.
     *
     * @return the spent outputs by this block.
     */
    List<UnspentTransactionOutput> getSpentOutputs(Sha256Hash sha256Hash) throws StorageException;

    /**
     * Gets the current chain head.
     *
     * @return The block at the head of the blockchain.
     */
    BlockMetadata getChainHead();

    /**
     * Sets the current chain head.
     *
     * @param metadata The block at the head of the blockchain.
     */
    void setChainHead(BlockMetadata metadata) throws StorageException;

    /**
     * Gets the transaction with the given hash.
     *
     * @param sha256Hash The transaction id.
     *
     * @return The transaction.
     */
    Transaction getTransaction(Sha256Hash sha256Hash) throws StorageException;

    /**
     * Gets the unspent output that matches the given transaction id and index inside that transaction.
     *
     * @param transactionId The transaction ID that contains the output.
     *
     * @return The transaction output, or null if the output is not available or was already spent.
     */
    UnspentTransactionOutput getUnspentOutput(Sha256Hash transactionId, int index) throws StorageException;

    /**
     * Gets all the unspent outputs of a given public key.
     *
     * @param address The address of the wallet to get the unspent outputs for.
     *
     * @return An array with all the unspent outputs related to a given public address.
     */
    public List<UnspentTransactionOutput> getUnspentOutputsForAddress(Address address) throws StorageException;

    /**
     * Adds the given unspent output to the database.
     *
     * @param output The unspent output to store in the system.
     */
    boolean addUnspentOutput(UnspentTransactionOutput output) throws StorageException;

    /**
     * Removes the unspent output transaction from the metadata provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    boolean removeUnspentOutput(Sha256Hash id, int index) throws StorageException;

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
    byte[] getRevertData(Block block, long height) throws StorageException;
}
