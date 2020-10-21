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
 * Represents a time interval.
 */
public class TimeSpan
{
    private long m_duration = 0;

    /**
     * Initializes a new instance of the TimeSpan class.
     */
    public TimeSpan()
    {
    }

    /**
     * Initializes a new instance of the TimeSpan class.
     *
     * @param milliseconds The duration in milliseconds.
     */
    public TimeSpan(long milliseconds)
    {
        this.m_duration = milliseconds;
    }

    /**
     * Initializes a new instance of the TimeSpan class.
     *
     * @param hours   The hours time interval component.
     * @param minutes The minutes time interval component.
     * @param seconds The seconds time interval component.
     */
    public TimeSpan(int hours, int minutes, int seconds)
    {
        addHours(hours);
        addMinutes(minutes);
        addSeconds(seconds);
    }

    /**
     * Initializes a new instance of the TimeSpan class.
     *
     * @param days    The days time interval component.
     * @param hours   The hours time interval component.
     * @param minutes The minutes time interval component.
     * @param seconds The seconds time interval component.
     */
    public TimeSpan(int days, int hours, int minutes, int seconds)
    {
        addDays(days);
        addHours(hours);
        addMinutes(minutes);
        addSeconds(seconds);
    }

    /**
     * Initializes a new instance of the TimeSpan class.
     *
     * @param days         The days time interval component.
     * @param hours        The hours time interval component.
     * @param minutes      The minutes time interval component.
     * @param seconds      The seconds time interval component.
     * @param milliseconds The milliseconds time interval component.
     */
    public TimeSpan(int days, int hours, int minutes, int seconds, int milliseconds)
    {
        addDays(days);
        addHours(hours);
        addMinutes(minutes);
        addSeconds(seconds);
        addMilliseconds(milliseconds);
    }

    /**
     * Gets the duration of this time span instance.
     *
     * @return The duration.
     */
    public long getDuration()
    {
        return m_duration;
    }

    /**
     * Sets the duration of this time span instance.
     *
     * @param duration The duration in milliseconds.
     */
    public void setDuration(long duration)
    {
        m_duration = duration;
    }

    /**
     * Adds a given number of days to the time spam.
     *
     * @param days The days component.
     *
     * @return A newly created time span.
     */
    public TimeSpan addDays(int days)
    {
        long duration = m_duration + (days * 24 * 60 * 60 * 1000);

        return new TimeSpan(duration);
    }

    /**
     * Adds a given number of hours to the time span.
     *
     * @param hours The hours component.
     *
     * @return A newly created time span.
     */
    public TimeSpan addHours(int hours)
    {
        long duration = m_duration + (hours * 60 * 60 * 1000);

        return new TimeSpan(duration);
    }

    /**
     * Adds a given number of minutes to the time span.
     *
     * @param minutes The minutes component.
     *
     * @return A newly created time span.
     */
    public TimeSpan addMinutes(int minutes)
    {
        long duration = m_duration + (minutes * 60 * 1000);

        return new TimeSpan(duration);
    }

    /**
     * Adds a given number of seconds to the time span.
     *
     * @param seconds The seconds component.
     *
     * @return A newly created time span.
     */
    public TimeSpan addSeconds(int seconds)
    {
        long duration = m_duration += seconds * 1000;

        return new TimeSpan(duration);
    }

    /**
     * Adds a given number of milliseconds to the time span.
     *
     * @param milliseconds The milliseconds component.
     *
     * @return A newly created time span.
     */
    public TimeSpan addMilliseconds(int milliseconds)
    {
        long duration = m_duration + milliseconds;

        return new TimeSpan(duration);
    }

    /**
     * Gets the total number of hours of this instance.
     *
     * @return The total number of hours.
     */
    public int getTotalHours()
    {
        return (int)(m_duration / 60 / 60 / 1000);
    }

    /**
     * Gets the total number of minutes in this instance.
     *
     * @return The total number of minutes.
     */
    public int getTotalMinutes()
    {
        return (int)(m_duration / 60 / 1000);
    }

    /**
     * Gets the total number of seconds of this instance.
     *
     * @return The total number of seconds.
     */
    public int getTotalSeconds()
    {
        return (int)Math.floor(m_duration / 1000);
    }

    /**
     * Gets the total number of milliseconds of this instance.
     *
     * @return The total number of milliseconds.
     */
    public long getTotalMilliseconds()
    {
        return m_duration;
    }

    /**
     * Gets the days component of this instance.
     *
     * @return The days component.
     */
    public int getDays()
    {
        return (int)(m_duration / 24 / 60 / 60 / 1000);
    }

    /**
     * Gets the hours component of this instance.
     *
     * @return The hours component.
     */
    public int getHours()
    {
        return (int)(m_duration / 60 / 60 / 1000) - (getDays() * 24);
    }

    /**
     * Gets the minutes component of this instance.
     *
     * @return The minutes component.
     */
    public int getMinutes()
    {
        return (int)(m_duration / 60 / 1000) - (getTotalHours() * 60);
    }

    /**
     * Gets the seconds component of this instance.
     *
     * @return The seconds component.
     */
    public int getSeconds()
    {
        return (int)(m_duration / 1000) - (getTotalMinutes() * 60);
    }

    /**
     * Gets the milliseconds component of this instance.
     *
     * @return The milliseconds component.
     */
    public int getMilliseconds()
    {
        return (int) m_duration - (getSeconds() * 1000);
    }

    /**
     * Equals operator of the time span class.
     *
     * @param object The object to be compared with.
     *
     * @return true if the objects are equal, otherwise, false.
     */
    @Override
    public boolean equals(Object object)
    {
        if (this == object)
            return true;

        if (!(object instanceof TimeSpan))
            return false;

        TimeSpan timeSpan = (TimeSpan) object;

        return Double.compare(timeSpan.m_duration, m_duration) == 0;
    }

    /**
     * Gets the hash code of this instance.
     *
     * @return The hash code of the instance.
     */
    @Override
    public int hashCode()
    {
        long temp = Double.doubleToLongBits(m_duration);

        return (int) (temp ^ (temp >>> 32));
    }

    /**
     * Creates a string representation of this object.
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format("%s:%s:%s.%s",
                Convert.padLeft(Integer.toString((getDays() * 24 + getHours())), 2, '0'),
                Convert.padLeft(Integer.toString(getMinutes()), 2, '0'),
                Convert.padLeft(Integer.toString(getSeconds()), 2, '0'),
                Convert.padLeft(Integer.toString(getMilliseconds()), 2, '0'));
    }
}