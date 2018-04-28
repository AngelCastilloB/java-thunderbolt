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
package com.thunderbolt.blockchain;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.ISerializable;
import com.thunderbolt.security.Hash;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.transaction.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Transaction data is permanently recorded in files called blocks. They can be thought of as the individual pages of a
 * city recorder's recordbook (where changes to title to real estate are recorded) or a stock transaction ledger. Blocks
 * are organized into a linear sequence over time (also known as the block chain).
 *
 * New transactions are constantly being processed by miners into new blocks which are added to the end of the chain.
 */
public class Block implements ISerializable
{
    public static final BigInteger LARGEST_HASH = BigInteger.ONE.shiftLeft(256);

    private BlockHeader            m_header       = new BlockHeader();
    private ArrayList<Transaction> m_transactions = new ArrayList<>();
    private List<byte[]>           m_merkleTree   = new ArrayList<>();

    /**
     * Creates a new empty block.
     */
    public Block()
    {
    }

    /**
     * Creates a new instance of the block class.
     *
     * @param header       The block header.
     * @param transactions The list of transactions.
     */
    public Block(BlockHeader header, ArrayList<Transaction> transactions)
    {
        m_header       = header;
        m_transactions = transactions;
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param buffer A buffer containing the transaction object Transaction object.
     */
    public Block(ByteBuffer buffer)
    {
        m_header = new BlockHeader(buffer);
        int transactionsCount = buffer.getInt();

        for (int i = 0; i < transactionsCount; ++i)
            m_transactions.add(new Transaction(buffer));
    }

    /**
     * Gets the block header.
     *
     * @return The block header.
     */
    public BlockHeader getHeader()
    {
        return m_header;
    }

    /**
     * Sets the block header.
     *
     * @param header The block header.
     */
    public void setHeader(BlockHeader header)
    {
        m_header = header;
    }

    /**
     * Gets the block transaction at the given index..
     *
     * @param index The transaction index in the block.
     *
     * @return The transactions at the given index.
     */
    public Transaction getTransaction(int index)
    {
        return m_transactions.get(index);
    }

    /**
     * Sets the blocks transactions.
     *
     * @param transactions The transactions in this block.
     */
    public void setTransactions(ArrayList<Transaction> transactions)
    {
        m_transactions = transactions;
        m_header.setMarkleRoot(calculateMerkleRoot());
    }

    /**
     * Sets the blocks transactions.
     *
     * @param transaction The transactions to be added to this block.
     */
    public void addTransactions(Transaction transaction)
    {
        m_transactions.add(transaction);
        m_header.setMarkleRoot(calculateMerkleRoot());
    }

    /**
     * Gets the hash of the block header.
     *
     * @return The hash of this block's header.
     *
     * @throws IOException If there is an error reading the block header serialized data.
     */
    public Hash getHeaderHash() throws IOException
    {
        return m_header.getHash();
    }

    /**
     * Returns the target difficulty in compact form
     *
     * @return      Target difficulty
     */
    public long getTargetDifficulty()
    {
        return m_header.getBits();
    }

    /**
     * Returns the target difficulty as a 256-bit value that can be compared to a SHA-256 hash.
     * Inside a block. the target is represented using the compact form.
     *
     * @return      The difficulty target
     */
    public BigInteger getTargetDifficultyAsInteger()
    {
        return Convert.decodeCompactBits(m_header.getBits());
    }

    /**
     * Returns the work represented by this block
     *
     * Work is defined as the number of tries needed to solve a block in the
     * average case.  As the target gets lower, the amount of work goes up.
     *
     * @return      The work represented by this block
     */
    public BigInteger getWork()
    {
        BigInteger target = getTargetDifficultyAsInteger();
        return LARGEST_HASH.divide(target.add(BigInteger.ONE));
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize() throws IOException
    {
        m_header.setMarkleRoot(calculateMerkleRoot());

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        byte[] transactionCountBytes = ByteBuffer.allocate(Integer.BYTES).putInt(m_transactions.size()).array();

        data.write(m_header.serialize());
        data.write(transactionCountBytes);

        for (Transaction transaction : m_transactions)
            data.write(transaction.serialize());

        return data.toByteArray();
    }

    /**
     * Calculates the Merkle root from the block transactions
     *
     * @return Merkle root
     */
    private Hash calculateMerkleRoot()
    {
        if (m_merkleTree.size() == 0)
            m_merkleTree = buildMerkleTree();

        return new Hash(m_merkleTree.get(m_merkleTree.size() - 1));
    }

    /**
     * Builds the Merkle tree from the block transactions
     *
     * @return List of byte arrays representing the nodes in the Merkle tree
     */
    private List<byte[]> buildMerkleTree()
    {
        ArrayList<byte[]> tree = new ArrayList<>();

        for (Transaction tx : m_transactions)
        {
            try
            {
                tree.add(Sha256Digester.digest(tx.serialize()).getData());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        int levelOffset = 0;

        for (int levelSize = m_transactions.size(); levelSize > 1; levelSize = (levelSize + 1) / 2)
        {
            for (int left=0; left<levelSize; left+=2)
            {
                int right = Math.min(left + 1, levelSize - 1);

                byte[] leftBytes  = tree.get(levelOffset + left);
                byte[] rightBytes = tree.get(levelOffset + right);

                byte[] both = Arrays.copyOf(leftBytes, leftBytes.length + rightBytes.length);
                System.arraycopy(rightBytes, 0, both, leftBytes.length, rightBytes.length);

                byte[] nodeHash   = Sha256Digester.digest(both).getData();

                tree.add(nodeHash);
            }

            levelOffset += levelSize;
        }

        return tree;
    }
}
