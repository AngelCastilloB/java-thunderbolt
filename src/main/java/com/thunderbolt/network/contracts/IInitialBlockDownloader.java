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

package com.thunderbolt.network.contracts;

/* IMPORTS *******************************************************************/

/* IMPLEMENTATION ************************************************************/

/**
 * Block downloader interface.
 */
public interface IInitialBlockDownloader
{
    /**
     * Starts the synchronization process our local chain with the peers.
     *
     * @return true if the synchronization process ended successfully; otherwise; false.
     */
    boolean synchronize();

    /**
     * Whether the syncing process ir over or not.
     *
     * @return Whether is currently syncing or not.
     */
    boolean isSyncing();

    /**
     * Gets the estimated process of the syncing process.
     *
     * @return The progress, a number between 0 and 100.
     */
    int getProgress();
}
