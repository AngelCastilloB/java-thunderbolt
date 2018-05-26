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
package com.thunderbolt.blockchain.contracts;

/* IMPORTS *******************************************************************/

import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;

/* IMPLEMENTATION ************************************************************/

/**
 * Commits and rolls back changes to the blockchain.
 */
public interface IBlockchainCommitter
{
    /**
     * Commits all the changes made by this block to the current blockchain state.
     *
     * 1.- Update the valid transactions pool
     * 2.- Update unspent transaction outputs database (coins).
     * 3.- Update spendable transactions and balance.
     *
     * @param metadata The metadata of the block we are going to apply the changes for.
     *
     * @return True if the changes were applied; otherwise; false.
     */
    boolean commit(BlockMetadata metadata) throws StorageException;

    /**
     * Rolls back all the changes previously made by this block to the current blockchain state.
     *
     * 1.- Re-insert all the transactions to the valid transactions pool. This transactions must now wait to be mined again by another block.
     * 2.- Remove all newly created unspent transaction outputs created by this block from the wallet and database (coins).
     * 3.- Re-insert spent transaction outputs to the wallet and database.
     *
     * @param metadata The metadata of the block we are going to revert the changes for.
     *
     * @return True if the changes were reverted; otherwise; false.
     */
    boolean rollback(BlockMetadata metadata) throws StorageException;
}
