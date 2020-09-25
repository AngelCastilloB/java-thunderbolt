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

/* IMPORTS *******************************************************************/

package com.thunderbolt.network;

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.transaction.*;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/* IMPLEMENTATION ************************************************************/

/**
 * NetworkParameters contains the information needed for working with an instantiation of a thunderbolt chain.
 */
public class NetworkParameters implements Serializable
{
    // Constants
    static private final int        PROTOCOL_VERSION                      = 1;
    static private final byte       MAIN_NET_SINGLE_SIGNATURE_PREFIX      = 0x10;
    static private final byte       MAIN_NET_MULTI_SIGNATURE_PREFIX       = 0x20;
    static private final int        MAIN_NET_TARGET_TIMESPAN              = 7 * 24 * 60 * 60;  // 1 week per difficulty cycle, on average.
    static private final int        MAIN_NET_TARGET_SPACING               = 2 * 60; // 2 minutes per block.
    static private final int        MAIN_NET_INTERVAL                     = MAIN_NET_TARGET_TIMESPAN / MAIN_NET_TARGET_SPACING;
    static private final long       MAIN_NET_PACKET_MAGIC                 = 0x746e6470;
    static private final long       MAIN_NET_SUBSIDY_HALVING_INTERVAL     = 210000;
    static private final BigInteger MAIN_NET_SUBSIDY_STARTING_VALUE       = BigInteger.valueOf(5000000000L);
    public static final long        MAIN_NET_COINBASE_MATURITY            = 0; // TODO: Add back the normal maturity. 100
    public static final long        MAIN_NET_MAX_BLOCK_SIZE               = 5242880; //5 mb

    // Instance fields
    private Block      m_genesisBlock;
    private BigInteger m_proofOfWorkLimit;
    private byte       m_singleSignatureAddressHeader;
    private byte       m_multiSignatureAddressHeader;
    private int        m_port;
    private long       m_packetMagic;
    private int        m_interval;
    private int        m_targetTimespan;
    private int        m_protocol;
    private long       m_blockSize;
    private long       m_coinbaseMaturity;

    /**
     * Creates the Genesis block.
     *
     * @return The genesis block.
     */
    public static Block createGenesis()
    {
        Block               genesisBlock = new Block();
        Transaction         transaction  = new Transaction();
        TransactionInput    input        = new TransactionInput();
        TransactionOutput   output       = new TransactionOutput();

        input.setIndex(Integer.MAX_VALUE);

        output.setAmount(MAIN_NET_SUBSIDY_STARTING_VALUE);
        output.setLockType(OutputLockType.SingleSignature);

        // The first eight bytes are for block height in coinbase transactions. This is to ensure unique hash for
        // coinbase transactions in case the are mined by the same miner.
        output.setLockingParameters(Hex.decode("022050C8868389B80FA27575412CF8D4C7C4BA5438FD86C98D6F93CB439426508E"));

        transaction.getInputs().add(input);
        transaction.getOutputs().add(output);

        String message = "Genesis block.";
        input.setUnlockingParameters(message.getBytes(StandardCharsets.US_ASCII));

        genesisBlock.addTransaction(transaction);

        genesisBlock.getHeader().setTimeStamp(1525003294);
        genesisBlock.getHeader().setTargetDifficulty(0x1dfffff8);
        genesisBlock.getHeader().setNonce(449327816);

        return genesisBlock;
    }

    /**
     * Gets the main net network parameters.
     *
     * @return The main net network parameters.
     *
     * @throws IOException IO exceptions.
     */
    public static NetworkParameters mainNet()
    {
        NetworkParameters parameters = new NetworkParameters();

        parameters.m_proofOfWorkLimit             = new BigInteger("00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
        parameters.m_port                         = 9567;
        parameters.m_packetMagic                  = MAIN_NET_PACKET_MAGIC;
        parameters.m_singleSignatureAddressHeader = MAIN_NET_SINGLE_SIGNATURE_PREFIX;
        parameters.m_multiSignatureAddressHeader  = MAIN_NET_MULTI_SIGNATURE_PREFIX;
        parameters.m_interval                     = MAIN_NET_INTERVAL;
        parameters.m_targetTimespan               = MAIN_NET_TARGET_TIMESPAN;
        parameters.m_genesisBlock                 = createGenesis();
        parameters.m_coinbaseMaturity             = MAIN_NET_COINBASE_MATURITY;
        parameters.m_blockSize                    = MAIN_NET_MAX_BLOCK_SIZE;

        String genesisHash = parameters.getGenesisBlock().getHeaderHash().toString();

        assert genesisHash.equals("00000004063B34C6FE99D1DB8A8C7F041B46487E64B0ED74C0EE8B7D4FA8F4E9") : genesisHash;

        return parameters;
    }

    /**
     * Gets the blocks pass between difficulty adjustment periods.
     *
     * @return How many blocks pass between difficulty adjustment periods.
     */
    public int getDifficulAdjustmentInterval()
    {
        return m_interval;
    }

    /**
     * How much time in seconds is supposed to pass between "interval" blocks.
     *
     * @return Time in seconds is supposed to pass between "interval" blocks.
     */
    public int getTargetTimespan()
    {
        return m_targetTimespan;
    }

    /**
     * Gets the genesis block for this network.
     *
     * @return The genesis block.
     */
    public Block getGenesisBlock()
    {
        return m_genesisBlock;
    }

    /**
     * Gets the proof of work limit for this network.
     *
     * @return The proof of work limit.
     */
    public BigInteger getProofOfWorkLimit()
    {
        return m_proofOfWorkLimit;
    }

    /**
     * Gets the single signature address prefix for this network.
     *
     * @return The address prefix.
     */
    public byte getSingleSignatureAddressHeader()
    {
        return m_singleSignatureAddressHeader;
    }

    /**
     * Gets the multi signature prefix for this network.
     *
     * @return The multi signature prefix.
     */
    public byte getMultiSignatureAddressHeader()
    {
        return m_multiSignatureAddressHeader;
    }

    /**
     * Gets the default communication port for this network.
     *
     * @return The port.
     */
    public int getPort()
    {
        return m_port;
    }

    /**
     * Returns the network maximum block size in bytes.
     *
     * @return The block size.
     */
    public long getBlockMaxSize()
    {
        return m_blockSize;
    }

    /**
     * Returns the maximum difficulty adjustment allowed.
     *
     * @return The maximum difficulty adjustment.
     */
    public int getMaxTimespanAdjustment()
    {
        return getTargetTimespan() * 4;
    }

    /**
     * Returns the minimum difficulty adjustment allowed.
     *
     * @return The minimum difficulty adjustment.
     */
    public int getMinTimespanAdjustment()
    {
        return getTargetTimespan() / 4;
    }

    /**
     * Gets the coinbase maturity, this is the minimum amount of blocks that need to be added after
     * a coinbase transaction was confirm to be able to spend the coinbase output..
     *
     * @return The coinbase maturity.
     */
    public long getCoinbaseMaturiry()
    {
        return m_coinbaseMaturity;
    }

    /**
     * Calculates the block subsidy for the given height.
     *
     * @param height The block height.
     *
     * @return The block subsidy.
     *
     * TODO: This function needs to be somewhere else.
     */
    public BigInteger getBlockSubsidy(long height)
    {
        // Genesis reward.
        if (height == 0)
            return MAIN_NET_SUBSIDY_STARTING_VALUE;

        int halvings = (int)(height / MAIN_NET_SUBSIDY_HALVING_INTERVAL);

        // Force block reward to zero when right shift is undefined.
        if (halvings >= 64)
            return BigInteger.ZERO;

        // Subsidy is cut in half every 210,000 blocks which will occur approximately every <N> years.
        BigInteger subsidy = MAIN_NET_SUBSIDY_STARTING_VALUE.shiftRight(halvings);

        return subsidy;
    }
}
