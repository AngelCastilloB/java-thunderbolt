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
package com.thunderbolt.persistence.datasource;

/* IMPORTS *******************************************************************/

import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Hash;

/* IMPLEMENTATION ************************************************************/

/**
 * Manifest of all the relevant blockchain related information. Blocks, transactions, inputs, outputs etc...
 */
public interface IBlockchainManifest
{
    /**
     * Gets the metadata entry from the manifest.
     *
     * @param id The hash of the block header.
     *
     * @return The block metadata.
     */
    BlockMetadata getBlockMetadata(Hash id);

    /**
     * Adds a block metadata entry to the manifest.
     *
     * @param metadata The metadata to be added.
     */
    boolean addBlockMetadata(BlockMetadata metadata);

    /**
     * Sets the block chain head in the manifest.
     *
     * @param metadata The metadata of the block chain head.
     */
    boolean setChainHead(BlockMetadata metadata);

    /**
     * Gets the block chain head metadata entry from the manifest.
     *
     * @return The block metadata.
     */
    BlockMetadata getChainHead();

    /**
     * Adds a transaction metadata entry to the manifest.
     *
     * @param metadata The metadata to be added.
     */
    void addTransactionMetadata(TransactionMetadata metadata);

    /**
     * Gets the metadata entry from the manifest.
     *
     * @param id The hash of the transaction.
     *
     * @return The transaction metadata.
     */
    TransactionMetadata getTransactionMetadata(Hash id);

    /**
     * Adds an unspent transaction to the database.
     *
     * @param output The unspent outputs to be added.
     */
    void addUnspentOutput(UnspentTransactionOutput output);

    /**
     * Gets an unspent transaction from the database.
     *
     * @param id    The transaction id that contains the unspent output.
     * @param index The index of the output inside the transaction.
     */
    UnspentTransactionOutput getUnspentOutput(Hash id, int index);
}
