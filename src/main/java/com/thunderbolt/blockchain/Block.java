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

import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Date;
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
    private static final Logger s_logger = LoggerFactory.getLogger(Block.class);

    public  static final BigInteger LARGEST_HASH       = BigInteger.ONE.shiftLeft(256);
    private static final long       ALLOWED_TIME_DRIFT = 2 * 60 * 60;

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
     * Gets the block transactions.
     *
     * @return The transactions in this block.
     */
    public List<Transaction> getTransactions()
    {
        return m_transactions;
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
     * Gets the number of transactions in this block.
     *
     * @return The number of transactions in the block.
     */
    public int getTransactionsCount()
    {
        return m_transactions.size();
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
     */
    public Hash getHeaderHash()
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
        return Block.unpackDifficulty(m_header.getBits());
    }

    /**
     * Decompress the difficulty target.
     *
     * Each block stores a packed representation (called "Bits") for its actual hexadecimal target. The target can be
     * derived from it via a predefined formula.
     *
     * For example, if the packed target in the block is 0x1b0404cb, the hexadecimal target is
     *
     * 0x0404cb * 2**(8*(0x1b - 3)) = 0x00000000000404CB000000000000000000000000000000000000000000000000
     *
     * Note that the 0x0404cb value is a signed value in this format. The largest legal value for this field
     * is 0x7fffff. To make a larger value you must shift it down one full byte. Also 0x008000 is the smallest
     * positive valid value.
     *
     * @param packedTarget The compressed difficulty target.
     *
     * @return The uncompressed difficulty target.
     */
    static public BigInteger unpackDifficulty(long packedTarget)
    {
        // Get the first 3 bytes of the difficulty.
        BigInteger last24bits = BigInteger.valueOf(packedTarget & 0x007FFFFFL);
        int        first8bits = (int)(packedTarget >> 24);

        return last24bits.shiftLeft(8 * (first8bits - 3));
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
    public byte[] serialize()
    {
        m_header.setMarkleRoot(calculateMerkleRoot());

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(m_header.serialize());
            data.write(NumberSerializer.serialize(m_transactions.size()));

            for (Transaction transaction : m_transactions)
                data.write(transaction.serialize());

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

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
     * Builds the Merkle tree from the block transactions.
     *
     * @return List of byte arrays representing the nodes in the Merkle tree
     */
    private List<byte[]> buildMerkleTree()
    {
        ArrayList<byte[]> tree = new ArrayList<>();

        for (Transaction tx : m_transactions)
        {
            tree.add(Sha256Digester.digest(tx.serialize()).getData());
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

                byte[] nodeHash = Sha256Digester.digest(both).getData();

                tree.add(nodeHash);
            }

            levelOffset += levelSize;
        }

        return tree;
    }

    /*

Block hash must satisfy claimed nBits proof of work
Block timestamp must not be more than two hours in the future
First transaction must be coinbase (i.e. only 1 input, with hash=0, n=-1), the rest must not be
For each transaction, apply "tx" checks 2-4

     */
    /**
     * Performs basic non contextual validations over the block data. This validations are naive and are not complete.
     * We need the context of the blockchain to make all the necessary validations. However we can rule out invalid
     * blocks very quick by checking a set of simple rules first.
     *
     * @return True if the block is valid; otherwise; false
     */
    public boolean isValid()
    {
        boolean isValid = isProofOfWorkValid();

        isValid &= isTimestampValid();

        if (m_transactions != null && m_transactions.isEmpty())
        {
            s_logger.error("The transaction list is empty.");
            return false;
        }

        isValid &= areTransactionsValid();
        isValid &= isMerkleRootValid();

        return isValid;
    }

    /**
     * Gets whether the proof of work for the block is valid.
     *
     * This function prove that the block was as difficult to create as it claims. However this is a naive validation,
     * since we need the context of the blockchain to prove that the difficulty of this block is correct.
     *
     * @return True if the proof of work matches the claimed difficulty target.
     */
    private boolean isProofOfWorkValid()
    {
        BigInteger target = getTargetDifficultyAsInteger();

        BigInteger hash = m_header.getHash().toBigInteger();

        if (hash.compareTo(target) > 0)
        {
            String.format("Hash is higher than target. Current hash %s; target %s",
                    m_header.getHash().toString(),
                    target.toString(16));

            return false;
        }

        return true;
    }

    /**
     * Gets whether the block timestamp is valid. A valid timestamp can not be two hours ahead of the current system time.
     *
     * @return True if the timestamp is valid; otherwise; false.
     */
    private boolean isTimestampValid()
    {
        long currentTime = System.currentTimeMillis();

        if (m_header.getTimeStamp() > currentTime + ALLOWED_TIME_DRIFT)
        {
            s_logger.error(String.format("Timestamp too far ahead in the future: Current time %s; Block time %s",
                    (new Date(currentTime).toLocalDate().toString()),
                    (new Date(m_header.getTimeStamp()).toLocalDate().toString())));

            return false;
        }

        return true;
    }

    /**
     * Gets whether the current Markle root is valid.
     *
     * @return true if the Markle root is valid; otherwise; false.
     */
    private boolean isMerkleRootValid()
    {
        Hash expectedRoot = calculateMerkleRoot();

        if (!expectedRoot.equals(m_header.getMarkleRoot()))
        {
            s_logger.error(String.format("Merkle hash invalid. Expected %s; Actual %s", expectedRoot, m_header.getMarkleRoot()));
            return false;
        }

        return true;
    }

    /**
     * Naive validation over the list fo transaction of this block.
     *
     * This function validates that the first transaction of the block is a coinbase transaction and that the rest
     * are not.
     *
     * @return True if the list of transaction are valid; otherwise; false.
     */
    private boolean areTransactionsValid()
    {
        if (!m_transactions.get(0).isCoinbase())
        {
            s_logger.error("First transaction is not a coinbase transaction.");
            return false;
        }

        for (int i = 1; i < m_transactions.size(); i++)
        {
            if (m_transactions.get(i).isCoinbase())
            {
                s_logger.error(String.format("Transaction %s is coinbase when it should not be.", i));
                return false;
            }

            if (m_transactions.get(i).isValid())
            {
                s_logger.error("Input '{}' of transaction '{}' in block '{}' is invalid.",
                        i,
                        m_transactions.get(i).getTransactionId(),
                        m_header.getHash());

                return false;
            }
        }

        return true;
    }

}
