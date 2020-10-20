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

package com.thunderbolt.mining.miners;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.mining.Job;
import com.thunderbolt.mining.contracts.IJobFinishListener;
import com.thunderbolt.mining.contracts.IMiner;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.security.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/* IMPLEMENTATION ************************************************************/

/**
 * Miner that uses the CPU to calculates the SHA-256 values.
 */
public class CpuMiner implements IMiner
{
    // Constants
    private static final int  THREAD_POOL_SIZE  = Runtime.getRuntime().availableProcessors();
    private static final int  BUSY_WAIT_DELAY   = 100; //ms
    private static final long MAX_UNSIGNED_INT  = 4294967295L;

    // Static fields.
    private static final Logger s_logger = LoggerFactory.getLogger(CpuMiner.class);

    // Instance fields.
    private volatile boolean               m_isRunning = false;
    private final BlockingQueue<Job>       m_jobQueue  = new LinkedBlockingQueue<>();
    private final List<IJobFinishListener> m_listeners = new ArrayList<>();
    private final ExecutorService          m_executor  = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private AtomicInteger                  m_active    = new AtomicInteger(0);
    private volatile boolean               m_cancelAll = false;
    private Thread                         m_thread    = null;

    /**
     * Starts the miner.
     *
     * @return true if the miner was started successfully; otherwise; false.
     */
    @Override
    public boolean start()
    {
        m_isRunning = true;
        m_thread = new Thread(this::run);
        m_thread.start();

        s_logger.info("CPU Miner Waiting for jobs.");
        return true;
    }

    /**
     * Stops the miner.
     */
    @Override
    public void stop()
    {
        m_isRunning = false;

        try
        {
            m_executor.shutdownNow();
            m_thread.join();
        }
        catch (InterruptedException e)
        {
            // Calling thread interrupted.
        }
    }

    /**
     * Gets whether this miner is running or not.
     *
     * @return true if is running; otherwise; false.
     */
    public boolean isRunning()
    {
        return m_isRunning;
    }

    /**
     * Cancels all current jobs
     */
    @Override
    public void cancelAllJobs()
    {
        m_cancelAll = true;

        while (m_active.get() > 0)
        {
            try
            {
                Thread.sleep(BUSY_WAIT_DELAY);
            }
            catch (InterruptedException e)
            {
                // Calling thread interrupted.
                break;
            }
        }
    }

    /**
     * Queue work on the miner.
     *
     * @param job The job to be work on.
     */
    @Override
    public void queueJob(Job job)
    {
        m_jobQueue.add(job);
    }

    /**
     * Adds an event listener to be notified when a Job is done.
     *
     * @param listener The event listener.
     */
    @Override
    public void addJobFinishListener(IJobFinishListener listener)
    {
        synchronized (m_listeners)
        {
            m_listeners.add(listener);
        }
    }

    /**
     * Removes an event listener.
     *
     * @param listener The event listener to be removed.
     */
    @Override
    public void removeJobFinishListener(IJobFinishListener listener)
    {
        synchronized (m_listeners)
        {
            m_listeners.remove(listener);
        }
    }

    /**
     * Runs the miner.
     */
    private void run()
    {
        try
        {
            while (m_isRunning)
            {
                if (m_jobQueue.size() > 0 && m_active.get() < THREAD_POOL_SIZE)
                {
                    Job job = m_jobQueue.take();
                    s_logger.info("Starting Job {}:\n - Midstate: {}\n - Data:     {}\n - Target:   {}",
                            job.getId(),
                            Convert.toHexString(job.getMidstate()),
                            Convert.toHexString(job.getData()),
                            Convert.padLeft(job.getTarget().toString(), 64, '0'));

                    m_executor.execute(() -> solve(job));
                }

                Thread.sleep(BUSY_WAIT_DELAY);
            }
        } catch (InterruptedException e)
        {
            s_logger.error("Miner interrupted.", e);
            cancelAllJobs();
            m_executor.shutdownNow();
        }

        if (m_executor.isShutdown())
        {
            cancelAllJobs();
            m_executor.shutdownNow();
        }
    }

    /**
     * Solves a mining job.
     */
    private void solve(Job job)
    {
        m_active.addAndGet(1);

        job.start();

        boolean solved          = false;
        byte[]  data            = job.getData();
        byte[]  midstate        = job.getMidstate();
        long    currentNonce    = job.getNonce();
        byte[]  serializedNonce = NumberSerializer.serialize((int)currentNonce);

        System.arraycopy(serializedNonce, 0, data, 12, serializedNonce.length);

        s_logger.debug(Convert.toHexString(serializedNonce));
        s_logger.debug(Convert.toHexString(data));
        s_logger.debug(Convert.toHexString(midstate));
        while (!solved && !m_cancelAll && !Thread.interrupted())
        {
            Sha256Digester digester = new Sha256Digester();
            Sha256Hash hash = Sha256Digester.digest(digester.continuePreviousHash(midstate, data)).reverse();

            solved = hash.toBigInteger().compareTo(job.getTarget().getTarget()) <= 0;
            if (solved)
            {
                job.setSolved(true);
                job.setNonce(currentNonce);

                s_logger.info("Job {}: Solved with hash: {}", job.getId(), Convert.toHexString(job.getHash().getData()));

                for (IJobFinishListener listener: m_listeners)
                    listener.onJobFinish(job);
            }
            // Copy new nonce.
            ++currentNonce;
            if (currentNonce > MAX_UNSIGNED_INT)
                break;

            serializedNonce = NumberSerializer.serialize((int)currentNonce);
            System.arraycopy(serializedNonce, 0, data, 12, serializedNonce.length);
        }

        job.finish();
        s_logger.info("Job {} ended.", job.getId());
        m_active.addAndGet(-1);
    }
}
