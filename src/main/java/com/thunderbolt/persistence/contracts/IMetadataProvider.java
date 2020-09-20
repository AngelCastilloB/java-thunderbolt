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

import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Hash;

import java.util.ArrayList;

/* IMPLEMENTATION ************************************************************/

/**
 * Contains all the relevant blockchain related metadata. Blocks, transactions, spent outputs etc...
 */
public interface IMetadataProvider
{
    /**
     * Gets the block metadata entry from the provider.
     *
     * @param id The hash of the block header.
     *
     * @return The block metadata.
     */
    BlockMetadata getBlockMetadata(Hash id) throws StorageException;

    /**
     * Adds a block metadata entry to the provider.
     *
     * @param metadata The metadata to be added.
     */
    boolean addBlockMetadata(BlockMetadata metadata) throws StorageException;

    /**
     * Sets the block chain head in the provider.
     *
     * @param metadata The metadata of the block chain head.
     */
    boolean setChainHead(BlockMetadata metadata) throws StorageException;

    /**
     * Gets the block chain head metadata entry from the provider.
     *
     * @return The block metadata.
     */
    BlockMetadata getChainHead() throws StorageException;

    /**
     * Adds a transaction metadata entry to the provider.
     *
     * @param metadata The metadata to be added.
     */
    void addTransactionMetadata(TransactionMetadata metadata) throws StorageException;

    /**
     * Gets the metadata entry from the provider.
     *
     * @param id The hash of the transaction.
     *
     * @return The transaction metadata.
     */
    TransactionMetadata getTransactionMetadata(Hash id) throws StorageException;

    /**
     * Adds an unspent transaction to the provider. This outputs is now spendable by any other transaction in
     * the mem pool.
     *
     * @param output The unspent outputs to be added.
     */
    boolean addUnspentOutput(UnspentTransactionOutput output) throws StorageException;

    /**
     * Gets an unspent transaction from the provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    UnspentTransactionOutput getUnspentOutput(Hash id, int index) throws StorageException;

    /**
     * Gets all the unspent outputs of a given public key.
     *
     * @param publicKey The public key of the wallet to get the unspent outputs for.
     *
     * @return An array with all the unspent outputs related to a given public address.
     */
    ArrayList<UnspentTransactionOutput> getUnspentOutputsForAddress(byte[] publicKey) throws StorageException;

    /**
     * Removes the unspent output transaction from the metadata provider.
     *
     * @param id    The id of the transaction that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    boolean removeUnspentOutput(Hash id, int index) throws StorageException;
}
