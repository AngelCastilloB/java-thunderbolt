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
package com.thunderbolt.common;

/* IMPLEMENTATION *********************************************************************************/

/**
 * Provides a set of methods and properties that you can use to accurately measure elapsed time.
 *
 * The Stopwatch calculates and retains the cumulative elapsed time across multiple time intervals,
 * until the instance is reset or restarted.
 */
public class Stopwatch
{
    private TimeSpan m_elapsed        = new TimeSpan(0);
    private TimeSpan m_startTimeStamp = new TimeSpan(0);
    private boolean  m_isRunning      = false;

    /**
     * Starts, or resumes, measuring elapsed time for an interval.
     */
    public void start()
    {
        if (m_isRunning)
            return;

        m_isRunning      = true;
        m_startTimeStamp = new TimeSpan(System.currentTimeMillis());
    }

    /**
     * Stops measuring elapsed time for an interval.
     */
    public void stop()
    {
        if (!m_isRunning)
            return;

        TimeSpan endTimeStamp = new TimeSpan(System.currentTimeMillis());

        m_elapsed   = new TimeSpan(m_elapsed.getDuration() + (endTimeStamp.getDuration() - m_startTimeStamp.getDuration()));
        m_isRunning = false;
    }

    /**
     * Gets the total elapsed time measured by the current instance.
     *
     * @return  A TimeSpan representing the total elapsed time measured by the current instance.
     */
    public TimeSpan getElapsedTime()
    {
        if (!m_isRunning)
            return m_elapsed;

        TimeSpan snapshot = new TimeSpan(System.currentTimeMillis());

        return new TimeSpan(m_elapsed.getDuration() + (snapshot.getDuration() - m_startTimeStamp.getDuration()));
    }

    /**
     * Stops time interval measurement and resets the elapsed time to zero.
     */
    public void reset()
    {
        m_isRunning = false;

        m_elapsed.setDuration(0);
    }

    /**
     * Stops time interval measurement, resets the elapsed time to zero, and starts measuring elapsed time.
     */
    public void restart()
    {
        m_elapsed.setDuration(0);

        m_isRunning      = true;
        m_startTimeStamp = new TimeSpan(System.currentTimeMillis());
    }

    /**
     * Indicates whether the stop watch timer is running.
     *
     * @return true if stop stop watch is currently running and measuring elapsed time for an interval;
     * otherwise false.
     */
    public boolean isRunning()
    {
        return m_isRunning;
    }
}