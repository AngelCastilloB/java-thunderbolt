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
package com.thunderbolt.transaction;

// IMPORTS ************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.security.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// IMPLEMENTATION ************************************************************/

/**
 * A transaction describes the move of funds from one address to another.
 */
public class Transaction implements ISerializable
{
    private static final Logger s_logger = LoggerFactory.getLogger(Transaction.class);

    // Constants
    // TODO: Move this constants to the network parameters. Add a way to access the network parameters (service locator?).
    private static final long MAX_BLOCK_SIZE    = 1000000;
    private static final long COIN              = 100000000;
    private static final long MAX_MONEY         = 21000000L * COIN;
    private static final long MAX_COINBASE_SIZE = 100;

    // Instance Fields
    private int                          m_version  = 0;
    private ArrayList<TransactionInput>  m_inputs   = new ArrayList<>();
    private ArrayList<TransactionOutput> m_outputs  = new ArrayList<>();
    private long                         m_lockTime = 0;

    /**
     * Creates a new instance of the Transaction class.
     */
    public Transaction()
    {
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param version  Transaction data format version.
     * @param inputs   A list of 1 or more inputs.
     * @param outputs  A list of 1 or more outputs.
     * @param lockTime UNIX timestamp at which this transaction unlocks.
     */
    public Transaction(int version, ArrayList<TransactionInput> inputs, ArrayList<TransactionOutput> outputs, long lockTime)
    {
        setVersion(version);
        setInputs(inputs);
        setOutputs(outputs);
        setLockTime(lockTime);
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param buffer A buffer containing the transaction object Transaction object.
     */
    public Transaction(ByteBuffer buffer)
    {
        setVersion(buffer.getInt());

        int inputsCount = buffer.getInt();

        for (int i = 0; i < inputsCount; ++i)
            getInputs().add(new TransactionInput(buffer));

        int outputCount = buffer.getInt();

        for (int i = 0; i < outputCount; ++i)
            getOutputs().add(new TransactionOutput(buffer));

        setLockTime(buffer.getLong());
    }

    /**
     * Gets the version of this transaction.
     *
     * @return The version of this transaction.
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * Sets the version of this transaction.
     *
     * @param version The version of this transaction.
     */
    public void setVersion(int version)
    {
        this.m_version = version;
    }

    /**
     * Gets the list of inputs in this transaction.
     *
     * @return The list of inputs.
     */
    public ArrayList<TransactionInput> getInputs()
    {
        return m_inputs;
    }

    /**
     * Sets the list of inputs in this transaction.
     *
     * @param inputs  The list of inputs.
     */
    public void setInputs(ArrayList<TransactionInput> inputs)
    {
        this.m_inputs = inputs;
    }

    /**
     * Gets the list of outputs in this transactions.
     *
     * @return The list of outputs.
     */
    public ArrayList<TransactionOutput> getOutputs()
    {
        return m_outputs;
    }

    /**
     * Sets the list of outputs in this transactions.
     *
     * @param outputs The list of outputs.
     */
    public void setOutputs(ArrayList<TransactionOutput> outputs)
    {
        this.m_outputs = outputs;
    }

    /**
     * Gets the lock time of the transaction.
     *
     * @return The lock time.
     */
    public long getLockTime()
    {
        return m_lockTime;
    }

    /**
     * Sets the lock time of the transaction.
     *
     * @param lockTime The lock time.
     */
    public void setLockTime(long lockTime)
    {
        this.m_lockTime = lockTime;
    }

    /**
     * Gets the transaction id of this transaction.
     *
     * @return The transaction id.
     */
    public Sha256Hash getTransactionId()
    {
        return Sha256Digester.digest(serialize());
    }

    /**
     * Performs basic non contextual validations over the transaction data. This validations are naive and are not complete.
     * We need the context of the blockchain to make all the necessary validations. However we can rule out invalid
     * transaction very quick by checking a set of simple rules first.
     * 
     * The following validations are performed by this methods:
     * 
     * <ol>
     * 	<li>Check syntactic correctness</li>
     * 	<li>Make sure neither in or out lists are empty</li>
     * 	<li>Size in bytes <= MAX_BLOCK_SIZE</li>
     * 	<li>Each output value, as well as the total, must be in legal money range</li>
     * 	<li>Make sure none of the inputs have hash=0, n=-1 (coinbase transactions)</li>
     * 	<li>Check that nLockTime <= INT_MAX</li>
     * 	<li>Check that coinbase parameters size <= 100 bytes</li>
     * 	<li>Check that no input reference the same output twice</li>
     * 	<li>Check that the transaction does not refers back to itself</li>
     * </ol>
     *
     * @return True if the transaction is valid; otherwise; false
     */
    public boolean isValid()
    {
        if (m_inputs.isEmpty())
        {
            s_logger.debug("The transaction contains no inputs. All transactions must at least have one input.");
            return false;
        }

        if (m_outputs.isEmpty())
        {
            s_logger.debug("The transaction contains no outputs. All transactions must at least have one input.");
            return false;
        }

        byte[] serializedData = serialize();

        if (serializedData.length > MAX_BLOCK_SIZE)
        {
            s_logger.debug("The transaction data is bigger than the maximum size allowed({} > {}).", serializedData.length, MAX_BLOCK_SIZE);
            return false;
        }

        BigInteger totalMoney = BigInteger.ZERO;

        int index = 0;
        for (TransactionOutput out : m_outputs)
        {
            if (out.getAmount().longValue() < 0)
            {
                s_logger.debug("Output ({}) of transaction {}, has negative value: {}", index, getTransactionId(), out.getAmount().longValue());
                return false;
            }

            if (out.getAmount().longValue() > MAX_MONEY)
            {
                s_logger.debug("Output ({}) of transaction {}, has more value than allowed: {}", index, getTransactionId(), out.getAmount().longValue());
                return false;
            }

            totalMoney = totalMoney.add(out.getAmount());
        }

        if (totalMoney.longValue() > MAX_MONEY)
        {
            s_logger.debug("Total value of transaction {}, has more value than allowed: {}", getTransactionId(), totalMoney.longValue());
            return false;
        }

        if ((getLockTime() < 0) || (getLockTime() > Integer.MAX_VALUE))
        {
            s_logger.debug("Lock time of transaction {}, is not in valid range: {}", getTransactionId(), getLockTime());
            return false;
        }

        if (isCoinbase())
        {
            int coinbaseSize = m_inputs.get(0).getUnlockingParameters().length;

            if (coinbaseSize > MAX_COINBASE_SIZE)
            {
                s_logger.debug("Unlocking parameters in coinbase transaction {}, has an invalid size: {}", getTransactionId(), MAX_COINBASE_SIZE);
                return false;
            }
        }
        else
        {
            int inputIndex = 0;
            for (TransactionInput input : m_inputs)
            {
                if (input.getIndex() < 0 || input.getReferenceHash().equals(new Sha256Hash()))
                {
                    s_logger.debug("Input {} for transaction ({}) has wrong reference to previous transaction", inputIndex, getTransactionId());
                    return false;
                }
                 ++inputIndex;
            }
        }

        Set<String> usedOuts = new HashSet<>();
        for (TransactionInput in : m_inputs)
        {
            String referredOut = String.format("%s-%s",in.getReferenceHash(), in.getIndex());

            if (usedOuts.contains(referredOut))
            {
                s_logger.debug("Transaction {} refers twice to the same output: {}", getTransactionId(), referredOut);
                return false;
            }

            usedOuts.add(referredOut);
        }

        int inputIndex = 0;
        for (TransactionInput in : getInputs() )
        {
            if (getTransactionId().equals(in.getReferenceHash()))
            {
                s_logger.debug("Transaction input {} refers to the containing transaction: {}", inputIndex, getTransactionId());
                return false;
            }

            ++inputIndex;
        }

        return true;
    }

    /**
     * A coinbase transaction is one that creates a new coin.
     */
    public boolean isCoinbase()
    {
        return m_inputs.get(0).isCoinBase();
    }

    /**
     * Compares this Hash instance to another one
     *
     * @param other The object to compare.
     *
     * @return True if the instances are equal; otherwise; false.
     */
    @Override
    public boolean equals(Object other)
    {
        return ((other instanceof Transaction) && getTransactionId().equals(((Transaction) other).getTransactionId()));
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(m_version));
            data.write(NumberSerializer.serialize(getInputs().size()));

            for (int i = 0; i < getInputs().size(); ++i)
                data.write(m_inputs.get(i).serialize());

            data.write(NumberSerializer.serialize(getOutputs().size()));
            for (int i = 0; i < getOutputs().size(); ++i)
                data.write(m_outputs.get(i).serialize());

            data.write(NumberSerializer.serialize(m_lockTime));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format(
                "{                        %n" +
                "  \"hash\":          %s, %n" +
                "  \"version\":       %s, %n" +
                "  \"lockTime\":      %s, %n" +
                "  \"inputs\":",
                getTransactionId(),
                m_version,
                m_lockTime) +
                Convert.toJsonArrayLikeString(m_inputs, 2) +
                "," +
                System.lineSeparator() +
                "  \"outputs\":" +
                Convert.toJsonArrayLikeString(m_outputs, 2) +
                "," +
                System.lineSeparator() +
                "}";
    }
}
