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

package com.thunderbolt.network.messages;

/* IMPLEMENTATION ************************************************************/

/**
 * List of possible services made available by this node.
 */
public enum NodeServices
{
    /**
     * 	This node can be asked for full blocks and transactions.
     */
    Network(0x00);

    // Instance fields.
    private final int m_value;

    /**
     * Initializes a new instance of the NodeServices class.
     *
     * @param value The enum value.
     */
    NodeServices(int value)
    {
        m_value = value;
    }

    /**
     * Gets the byte value of this enum instance.
     *
     * @return The byte value.
     */
    public int getValue()
    {
        return m_value;
    }

    /**
     * Gets an enum value from a byte.
     *
     * @param value The int to be casted.
     *
     * @return The enum value.
     */
    static public NodeServices from(int value)
    {
        return NodeServices.values()[value];
    }
}
