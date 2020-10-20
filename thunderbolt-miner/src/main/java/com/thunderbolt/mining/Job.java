/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.mining;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.security.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * A unit of work for the miners. Its compose by the midstate of the hash of a block header, and the remaining
 * data to be hashed.
 */
public class Job
{
    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(Job.class);

    // Constants
    private static final int MIDSTATE_SIZE = 32;
    private static final int DATA_SIZE     = 16;

    // Private fields
    private byte[]     m_midstate = new byte[MIDSTATE_SIZE];
    private byte[]     m_data     = new byte[DATA_SIZE];
    private short      m_id       = 0;
    private boolean    m_solved   = false;
    private long       m_nonce    = 0;
    private long       m_target   = 0;
    private Sha256Hash m_hash     = new Sha256Hash();
    private Stopwatch  m_watch    = new Stopwatch();

    /**
     * Initializes a new instance of the Job class.
     *
     * @param midstate The midstate of the hash block header.
     * @param data     The remaining data to be hashed.
     * @param id       The id assigned to this Job.
     */
    public Job(byte[] midstate, byte[] data, short id)
    {
        m_midstate = midstate;
        m_data = data;
        setId(id);

        ByteBuffer dataBuffer = ByteBuffer.wrap(m_data);
        dataBuffer.position(8);

        m_target = Integer.reverseBytes(dataBuffer.getInt()) & 0xffffffffL;
        m_nonce  = dataBuffer.getInt() & 0xffffffffL;
    }

    /**
     * Initializes a new instance of the Job class.
     *
     * @param payload A bytebuffer containing the Job data.
     * @param id       The id assigned to this Job.
     */
    public Job(ByteBuffer payload, short id)
    {
        payload.get(m_midstate);
        payload.get(m_data);
        setId(id);

        ByteBuffer dataBuffer = ByteBuffer.wrap(m_data);
        dataBuffer.position(8);
        m_target = Integer.reverseBytes(dataBuffer.getInt()) & 0xffffffffL; // Little endian.
        m_nonce  = dataBuffer.getInt() & 0xffffffffL;
    }

    /**
     * Initializes a new instance of the Job class.
     *
     * @param payload A byte array containing the Job data.
     * @param id      The id assigned to this Job.
     */
    public Job(byte[] payload, short id)
    {
        this(ByteBuffer.wrap(payload), id);
    }

    /**
     * Gets the midstate part of the Job.
     *
     * @return The midstate.
     */
    public byte[] getMidstate()
    {
        return m_midstate;
    }

    /**
     * Sets the midstate part of the Job.
     *
     * @param midstate The midstate.
     */
    public void setMidstate(byte[] midstate)
    {
        m_midstate = midstate;
    }

    /**
     * Gets the data part of the Job.
     *
     * @return The data.
     */
    public byte[] getData()
    {
        return m_data;
    }

    /**
     * Sets the data part of the block.
     *
     * @param data The data to be set.
     */
    public void setData(byte[] data)
    {
        m_data = data;
    }

    /**
     * Gets the id assigned to this Job.
     *
     * @return the id.
     */
    public short getId()
    {
        return m_id;
    }

    /**
     * Sets the id assigned to this Job.
     *
     * @param id The id.
     */
    public void setId(short id)
    {
        m_id = id;
    }

    /**
     * Gets whether this Job unit is solved or not.
     *
     * @return true if the Job is solved; otherwise; false.
     */
    public boolean isSolved()
    {
        return m_solved;
    }

    /**
     * Sets this Job as solved.
     *
     * @param solved  true if the Job is solved; otherwise; false.
     */
    public void setSolved(boolean solved)
    {
        m_solved = solved;
    }

    /**
     * Gets the nonce that solved the Job.
     *
     * @return the nonce.
     */
    public int getNonce()
    {
        return (int)m_nonce;
    }

    /**
     * Sets the nonce that solves the Job.
     *
     * @param nonce The nonce that solves the Job.
     */
    public void setNonce(long nonce)
    {
        m_nonce = nonce;
        byte[] data = m_data.clone();

        byte[] serializedNonce = NumberSerializer.serialize((int)m_nonce);
        System.arraycopy(serializedNonce, 0, data, 12, serializedNonce.length);

        s_logger.debug(Convert.toHexString(serializedNonce));
        s_logger.debug(Convert.toHexString(data));
        s_logger.debug(Convert.toHexString(m_midstate));
        Sha256Digester digester = new Sha256Digester();

        m_hash = Sha256Digester.digest(digester.continuePreviousHash(m_midstate, data)).reverse();
    }

    /**
     * Gets the target difficulty for this Job.
     *
     * @return The target difficulty.
     */
    public Difficulty getTarget()
    {
        return new Difficulty(m_target);
    }

    /**
     * Gets the resulting hash for this job.
     *
     * @return The final hash.
     */
    public Sha256Hash getHash()
    {
        return m_hash;
    }

    /**
     * Sets the resulting hash.
     *
     * @param hash The hash.
     */
    public void setHash(Sha256Hash hash)
    {
        m_hash = hash;
    }

    /**
     * The job starts.
     */
    public void start()
    {
        m_watch.restart();
    }

    /**
     * the job jas finish.
     */
    public void finish()
    {
        m_watch.stop();
    }

    /**
     * Gets the time elapsed since the job started until it finishes.
     *
     * @return The elapsed time since the job started.
     */
    public TimeSpan getElapsed()
    {
        return m_watch.getElapsedTime();
    }

}
