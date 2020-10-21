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

package com.thunderbolt.mining.contracts;

/* IMPLEMENTATION ************************************************************/

import com.thunderbolt.mining.Job;

/**
 * Cryptocurrency miner interface. This objects apply SHA-256 to the given workload until they find the nonce
 * that solves the block.
 */
public interface IMiner
{
    /**
     * Starts the miner.
     *
     * @return true if the miner was started successfully; otherwise; false.
     */
    boolean start();

    /**
     * Stops the miner.
     */
    void stop();

    /**
     * Gets the number of active jobs.
     *
     * @return The number of active jobs.
     */
    int getActiveJobs();

    /**
     * Gets whether this miner is running or not.
     *
     * @return true if is running; otherwise; false.
     */
    boolean isRunning();

    /**
     * Cancels all current jobs
     */
    void cancelAllJobs();

    /**
     * Queue work on the miner.
     *
     * @param job The job to be work on.
     */
    void queueJob(Job job);

    /**
     * Adds an event listener to be notified when a Job is done.
     *
     * @param listener The event listener.
     */
    void addJobFinishListener(IJobFinishListener listener);

    /**
     * Removes an event listener.
     *
     * @param listener The event listener to be removed.
     */
    void removeJobFinishListener(IJobFinishListener listener);
}
